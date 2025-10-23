package tech.kaffa.portrait.codegen.provider

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import tech.kaffa.portrait.codegen.utils.BytecodeImplementation

/**
 * ByteCode implementation for per-shard static methods that perform hash/equals dispatch.
 */
class PortraitForFirstCharMethodImpl(
    private val shardMap: Map<String, String>
) : BytecodeImplementation() {

    override fun apply(
        mv: MethodVisitor,
        context: Implementation.Context,
        method: MethodDescription
    ): ByteCodeAppender.Size {
        if (shardMap.isEmpty()) {
            mv.visitInsn(Opcodes.ACONST_NULL)
            mv.visitInsn(Opcodes.ARETURN)
            return ByteCodeAppender.Size(1, 1)
        }

        val defaultLabel = Label()

        mv.visitVarInsn(Opcodes.ALOAD, 0)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "hashCode",
            "()I",
            false
        )

        val hashGroups = shardMap.entries.groupBy { (className, _) -> className.hashCode() }
        val sortedHashes = hashGroups.keys.sorted()
        val hashLabels = sortedHashes.associateWith { Label() }

        mv.visitLookupSwitchInsn(
            defaultLabel,
            sortedHashes.toIntArray(),
            sortedHashes.map { hashLabels.getValue(it) }.toTypedArray()
        )

        sortedHashes.forEach { hash ->
            val entries = hashGroups.getValue(hash)
            mv.visitLabel(hashLabels.getValue(hash))

            entries.forEachIndexed { index, (className, portraitClassName) ->
                val nextLabel = if (index < entries.size - 1) Label() else defaultLabel

                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitLdcInsn(className)
                mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/String",
                    "equals",
                    "(Ljava/lang/Object;)Z",
                    false
                )
                mv.visitJumpInsn(Opcodes.IFEQ, nextLabel)

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
                mv.visitInsn(Opcodes.ARETURN)

                if (index < entries.size - 1) {
                    mv.visitLabel(nextLabel)
                }
            }
        }

        mv.visitLabel(defaultLabel)
        mv.visitInsn(Opcodes.ACONST_NULL)
        mv.visitInsn(Opcodes.ARETURN)

        return ByteCodeAppender.Size(3, 1)
    }
}