package kaffa.portrait.codegen.provider

import kaffa.portrait.PClass
import kaffa.portrait.provider.PortraitProvider
import kaffa.portrait.codegen.PortraitGenerator
import kaffa.portrait.codegen.portrait.PortraitClassFactory
import kaffa.portrait.codegen.utils.BytecodeImplementation
import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory

/**
 * Factory for generating a PortraitProvider that provides all generated Portrait classes.
 *
 * This factory creates a provider class that implements PortraitProvider and contains
 * static mappings to all the generated Portrait classes from the current codegen execution.
 */
class GeneratedPortraitProviderFactory(
    private val byteBuddy: ByteBuddy,
    private val typePool: TypePool
) {
    private val logger = LoggerFactory.getLogger(GeneratedPortraitProviderFactory::class.java)

    data class Result(
        override val dynamicType: DynamicType,
        val providerClassName: String
    ) : PortraitGenerator.GeneratedClass

    /**
     * Creates a GeneratedPortraitProvider class that provides all the given portrait classes.
     *
     * @param generatedPortraits List of generated portrait results with their DynamicTypes
     * @param packageName Package name for the generated provider (default: kaffa.portrait.generated)
     * @return Result containing the generated provider class
     */
    fun make(
        generatedPortraits: List<PortraitClassFactory.Result>,
        packageName: String = "kaffa.portrait.generated"
    ): Result {
        val providerClassName = "$packageName.GeneratedPortraitProvider"

        // Create mapping from original class name to portrait class name
        val portraitMap = generatedPortraits.associate { result ->
            result.superType.name to result.dynamicType.typeDescription.name
        }

        val builder = byteBuddy
            .subclass(PortraitProvider::class.java)
            .name(providerClassName)
            .defineMethod(
                "priority",
                TypeDescription.ForLoadedType.of(Int::class.javaPrimitiveType!!),
                Visibility.PUBLIC
            )
            .intercept(FixedValue.value(150)) // Priority 150 (between JVM=100 and WellKnown=200)
            .defineMethod("forName", PClass::class.java, Visibility.PUBLIC)
            .withParameters(String::class.java)
            .intercept(PortraitProviderForNameImpl(portraitMap))
            .visit(
                AsmVisitorWrapper.ForDeclaredMethods()
                    .writerFlags(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            )

        val dynamicType = builder.make(typePool)

        return Result(dynamicType, providerClassName)
    }

    /**
     * ByteCode implementation for the forName method that generates a string-switching dispatch.
     */
    private class PortraitProviderForNameImpl(
        private val portraitMap: Map<String, String>
    ) : BytecodeImplementation() {

        override fun apply(
            mv: MethodVisitor,
            context: Implementation.Context,
            method: MethodDescription
        ): ByteCodeAppender.Size {
            val classNameToPortrait = portraitMap.entries.toList()

            if (classNameToPortrait.isEmpty()) {
                // If no portraits, just return null
                mv.visitInsn(Opcodes.ACONST_NULL)
                mv.visitInsn(Opcodes.ARETURN)
                return ByteCodeAppender.Size(1, 1)
            }

            val defaultLabel = Label()
            val wrapLabel = Label()
            val endLabel = Label()

            // Group entries by hash code for LOOKUPSWITCH optimization
            val hashGroups = classNameToPortrait.groupBy { (className, _) -> className.hashCode() }
            val sortedHashes = hashGroups.keys.sorted()

            // Generate LOOKUPSWITCH on hashCode()
            mv.visitVarInsn(Opcodes.ALOAD, 1) // Load className parameter
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false)

            val hashLabels = sortedHashes.associateWith { Label() }
            val keys = sortedHashes.toIntArray()
            val labels = sortedHashes.map { hashLabels[it]!! }.toTypedArray()

            mv.visitLookupSwitchInsn(defaultLabel, keys, labels)

            // Generate comparison and instantiation for each hash bucket
            sortedHashes.forEach { hash ->
                val entries = hashGroups[hash]!!
                mv.visitLabel(hashLabels[hash]!!)

                // For each entry with this hash, do an equals check
                entries.forEachIndexed { idx, (className, portraitClassName) ->
                    val nextLabel = if (idx < entries.size - 1) Label() else defaultLabel

                    // Load className and compare with constant
                    mv.visitVarInsn(Opcodes.ALOAD, 1)
                    mv.visitLdcInsn(className)
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false)
                    mv.visitJumpInsn(Opcodes.IFEQ, nextLabel)

                    // Match found - instantiate the portrait
                    val portraitInternalName = portraitClassName.replace('.', '/')
                    mv.visitTypeInsn(Opcodes.NEW, portraitInternalName)
                    mv.visitInsn(Opcodes.DUP)
                    mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        portraitInternalName,
                        "<init>",
                        "()V",
                        false
                    )
                    mv.visitJumpInsn(Opcodes.GOTO, wrapLabel)

                    if (idx < entries.size - 1) {
                        mv.visitLabel(nextLabel)
                    }
                }
            }

            // Default case: return null
            mv.visitLabel(defaultLabel)
            mv.visitInsn(Opcodes.ACONST_NULL)
            mv.visitJumpInsn(Opcodes.GOTO, endLabel)

            // Shared code: wrap StaticPortrait in StaticPClass (written once, reused by all paths)
            mv.visitLabel(wrapLabel)
            // Stack on entry: [StaticPortrait instance]
            val staticPClassInternalName = "kaffa/portrait/aot/StaticPClass"
            mv.visitTypeInsn(Opcodes.NEW, staticPClassInternalName)
            mv.visitInsn(Opcodes.DUP_X1) // Stack: [StaticPClass, StaticPortrait, StaticPClass]
            mv.visitInsn(Opcodes.SWAP)   // Stack: [StaticPClass, StaticPClass, StaticPortrait]
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                staticPClassInternalName,
                "<init>",
                "(Lkaffa/portrait/aot/StaticPortrait;)V",
                false
            )
            // Stack: [StaticPClass instance]

            mv.visitLabel(endLabel)
            mv.visitInsn(Opcodes.ARETURN)

            return ByteCodeAppender.Size(2, 2)
        }
    }
}
