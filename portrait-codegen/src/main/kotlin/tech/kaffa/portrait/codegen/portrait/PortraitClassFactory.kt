package tech.kaffa.portrait.codegen.portrait

import kotlinx.metadata.ClassKind
import kotlinx.metadata.KmClass
import kotlinx.metadata.Modality
import kotlinx.metadata.isData
import kotlinx.metadata.jvm.KotlinClassMetadata
import kotlinx.metadata.kind
import kotlinx.metadata.modality
import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import tech.kaffa.portrait.aot.ProxyMethodIndexer
import tech.kaffa.portrait.aot.StaticPortrait
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.serde.MetadataSerializer
import tech.kaffa.portrait.codegen.PortraitGenerator
import tech.kaffa.portrait.codegen.proxy.ProxyClassFactory
import tech.kaffa.portrait.codegen.utils.interfaceNames
import tech.kaffa.portrait.codegen.utils.superclassNameOrNull
import tech.kaffa.portrait.codegen.utils.toAnnotationEntries
import tech.kaffa.portrait.codegen.utils.toPConstructorEntry
import tech.kaffa.portrait.codegen.utils.toPFieldEntry
import tech.kaffa.portrait.codegen.utils.toPMethodEntry
import tech.kaffa.portrait.proxy.ProxyHandler

class PortraitClassFactory(
    private val byteBuddy: ByteBuddy,
    private val typePool: TypePool,
    private val generatedProxies: MutableMap<String, ProxyClassFactory.Result>
) {
    private val logger = LoggerFactory.getLogger(PortraitClassFactory::class.java)

    private fun generatedClassName(superType: TypeDescription): String {
        val originalName = superType.name
        return if (originalName.startsWith("java.")) {
            "tech.kaffa.portrait.generated.jdk.$originalName\$Portrait"
        } else {
            "$originalName\$Portrait"
        }
    }

    data class Result(
        val superType: TypeDescription,
        override val dynamicType: DynamicType
    ) : PortraitGenerator.GeneratedClass

    fun make(superType: TypeDescription): Result {
        val kotlinMetadata = extractKotlinMetadata(superType)

        val className = generatedClassName(superType)

        val (constructors, methods) = superType.declaredMethods
            .asSequence()
            .filter { it.isPublic }
            .withIndex()
            .partition { it.value.isConstructor }

        val fields = superType.declaredFields
            .asSequence()
            .filter { it.isPublic }
            .withIndex()
            .toList()

        var builder = byteBuddy
            .subclass(
                TypeDescription.Generic.Builder
                    .parameterizedType(TypeDescription.ForLoadedType.of(StaticPortrait::class.java), superType)
                    .build()
            )
            .name(className)
            .defineMethod("getClassName", String::class.java, Visibility.PUBLIC)
            .intercept(FixedValue.value(superType.name))

        // Add Kotlin object instance support
        try {
            if (kotlinMetadata?.kind == ClassKind.OBJECT) {
                builder = builder
                    .defineMethod("getObjectInstance", Object::class.java, Visibility.PUBLIC)
                    .intercept(InstanceKtMethodImpl(superType))
            }
        } catch (e: Exception) {
            logger.debug(
                "Failed to extract Kotlin object instance information for class '${superType.name}': ${e.message}",
                e
            )
        }

        // Add constructor support (skip for abstract/interface classes)
        if (constructors.isNotEmpty() && !superType.isAbstract && !superType.isInterface) {
            builder = builder
                .defineMethod("invokeConstructor", Object::class.java, Visibility.PUBLIC)
                .withParameters(Int::class.javaPrimitiveType, Array<Any>::class.java)
                .intercept(InstantiatorMethodImpl(superType, constructors))
        }

        // Add method support
        if (methods.isNotEmpty()) {
            builder = builder
                .defineMethod("invokeMethod", Object::class.java, Visibility.PUBLIC)
                .withParameters(Int::class.javaPrimitiveType, Object::class.java, Array<Any>::class.java)
                .intercept(InvokerMethodImpl(superType, methods))
        }

        // Add field support
        if (fields.isNotEmpty()) {
            builder = builder
                .defineMethod("getFieldValue", Object::class.java, Visibility.PUBLIC)
                .withParameters(Int::class.javaPrimitiveType, Object::class.java)
                .intercept(FieldGetterMethodImpl(superType, fields))
                .defineMethod("setFieldValue", Void.TYPE, Visibility.PUBLIC)
                .withParameters(Int::class.javaPrimitiveType, Object::class.java, Object::class.java)
                .intercept(FieldSetterMethodImpl(superType, fields))
        }

        val proxy = generatedProxies[superType.name]

        if (proxy != null) {
            val proxyConstructor = proxy.dynamicType.typeDescription
                .declaredMethods
                .first { it.isConstructor && it.parameters.size == 2 }

            builder = builder
                .defineMethod("createProxy", Object::class.java, Visibility.PUBLIC)
                .withParameters(ProxyMethodIndexer::class.java, ProxyHandler::class.java)
                .intercept(MethodCall.construct(proxyConstructor).withAllArguments())
        }

        val metadata = createClassEntry(
            superType, kotlinMetadata, constructors, methods, fields, proxy?.proxiedMethods
        )

        builder = builder
            .defineMethod("getMetadata", String::class.java, Visibility.PUBLIC)
            .intercept(FixedValue.value(MetadataSerializer().serialize(metadata)))

        return Result(
            superType,
            builder.visit(
                AsmVisitorWrapper.ForDeclaredMethods()
                    .writerFlags(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            ).make(typePool)
        )
    }

    private fun extractKotlinMetadata(superType: TypeDescription): KmClass? {
        try {
            val metadataAnnotation = superType.declaredAnnotations.ofType(Metadata::class.java)?.load()
                ?: return null

            return when (val kmMetadata = KotlinClassMetadata.read(metadataAnnotation)) {
                is KotlinClassMetadata.Class -> kmMetadata.kmClass
                else -> null
            }
        } catch (e: Exception) {
            logger.debug("Failed to extract Kotlin metadata for class '${superType.name}': ${e.message}", e)
            return null
        }
    }

    private fun createClassEntry(
        typeDescription: TypeDescription,
        kotlinMetadata: KmClass?,
        constructors: List<IndexedValue<MethodDescription>>,
        methods: List<IndexedValue<MethodDescription>>,
        fields: List<IndexedValue<FieldDescription>>,
        proxyMethods: List<IndexedValue<MethodDescription>>?
    ): PClassEntry {
        return PClassEntry(
            simpleName = typeDescription.simpleName,
            qualifiedName = typeDescription.typeName.takeIf { it.contains(".") },
            isAbstract = typeDescription.isAbstract,
            isSealed = kotlinMetadata?.modality == Modality.SEALED,
            isData = kotlinMetadata?.isData ?: false,
            isCompanion = kotlinMetadata?.kind == ClassKind.COMPANION_OBJECT,
            isObject = kotlinMetadata?.kind == ClassKind.OBJECT,
            javaClassName = typeDescription.typeName,
            superclassName = typeDescription.superclassNameOrNull(),
            interfaceNames = typeDescription.interfaceNames(),
            annotations = typeDescription.toAnnotationEntries(),
            constructors = constructors.map { (_, methodDescription) ->
                methodDescription.toPConstructorEntry(typeDescription.typeName)
            },
            declaredMethods = methods.map { (_, methodDescription) ->
                methodDescription.toPMethodEntry(typeDescription.typeName)
            },
            declaredFields = fields.map { (_, fieldDescription) ->
                fieldDescription.toPFieldEntry(typeDescription.typeName)
            },
            proxyMethods = proxyMethods?.map { (_, methodDescription) ->
                methodDescription.toPMethodEntry(methodDescription.declaringType.typeName)
            } ?: emptyList()
        )
    }
}
