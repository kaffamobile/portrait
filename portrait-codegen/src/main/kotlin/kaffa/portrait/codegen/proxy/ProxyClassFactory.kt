package kaffa.portrait.codegen.proxy

import kaffa.portrait.PMethod
import kaffa.portrait.aot.ProxyMethodIndexer
import kaffa.portrait.codegen.PortraitGenerator
import kaffa.portrait.proxy.ProxyHandler
import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FieldAccessor
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.MethodCall
import net.bytebuddy.implementation.bytecode.assign.Assigner
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.matcher.ElementMatcher
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import net.bytebuddy.matcher.ElementMatchers.`is` as matchExactly

class ProxyClassFactory(private val byteBuddy: ByteBuddy, private val typePool: TypePool) {
    private val logger = LoggerFactory.getLogger(ProxyClassFactory::class.java)

    data class Result(
        val superType: TypeDescription,
        override val dynamicType: DynamicType,
        val proxiedMethods: List<IndexedValue<MethodDescription.InDefinedShape>>,
    ) : PortraitGenerator.GeneratedClass

    fun make(superType: TypeDescription): Result {
        val proxyClassName = "${superType.name}\$Proxy"

        require(superType.isInterface) {
            "Only interfaces are supported for proxy generation. Class: ${superType.name}"
        }

        // Create proxy class builder using extension
        var builder = byteBuddy
            .subclass(Object::class.java, ConstructorStrategy.Default.NO_CONSTRUCTORS)
            .name(proxyClassName)
            .implement(superType)
            .defineField("indexer", ProxyMethodIndexer::class.java, Visibility.PRIVATE)
            .defineField("handler", ProxyHandler::class.java, Visibility.PRIVATE)
            .defineConstructor(Visibility.PUBLIC)
            .withParameters(ProxyMethodIndexer::class.java, ProxyHandler::class.java)
            .intercept(
                MethodCall.invoke(Object::class.java.getDeclaredConstructor())
                    .onSuper()
                    .andThen(FieldAccessor.ofField("indexer").setsArgumentAt(0))
                    .andThen(FieldAccessor.ofField("handler").setsArgumentAt(1))
            )

        // Create method index mapping from collected methods
        val proxyMethods = (methodsFromObject.asSequence() + collectInterfaceMethods(superType))
            .distinct()
            .withIndex()
            .toList()

        // Implement each method by delegating to ProxyHandler
        for ((index, method) in proxyMethods) {
            try {
                builder = builder.method(matchExactly(method)).intercept(proxyMethod(index))
            } catch (e: Exception) {
                logger.warn("Could not implement method ${method.name} in proxy for ${superType.name}: ${e.message}", e)
            }
        }

        return Result(
            superType,
            builder.visit(
                AsmVisitorWrapper.ForDeclaredMethods()
                    .writerFlags(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            ).make(typePool),
            proxyMethods
        )
    }

    private fun collectInterfaceMethods(type: TypeDescription): Sequence<MethodDescription.InDefinedShape> = sequence {
        yieldAll(type.declaredMethods.filter { !it.isDefaultMethod })
        for (it in type.interfaces.asErasures()) yieldAll(collectInterfaceMethods(it))
    }

    private fun proxyMethod(methodIndex: Int): Implementation {
        return MethodCall.invoke(
            ProxyHandler::class.java.getMethod(
                "invoke",
                Object::class.java,
                PMethod::class.java,
                Array<Any>::class.java
            )
        )
            .onField("handler")
            .withThis()
            .withMethodCall(
                MethodCall
                    .invoke(
                        ProxyMethodIndexer::class.java.getMethod("method", Int::class.javaPrimitiveType)
                    )
                    .onField("indexer")
                    .with(methodIndex)
            )
            .withArgumentArray()
            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC)
    }

    companion object {
        private val methodsFromObject by lazy {
            val matchers = listOf<ElementMatcher<MethodDescription>>(
                ElementMatchers.isEquals(),
                ElementMatchers.isHashCode(),
                ElementMatchers.isToString()
            )
            TypeDescription.ForLoadedType
                .of(Any::class.java)
                .declaredMethods
                .filter { m -> matchers.any { it.matches(m) } }
        }
    }
}
