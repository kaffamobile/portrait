package tech.kaffa.portrait.codegen.provider

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import tech.kaffa.portrait.codegen.utils.BytecodeImplementation

/**
 * ByteCode implementation for the top-level forName method that shards dispatch by first character.
 */
class PortraitForNameMethodImpl(
    private val firstChars: IntArray,
    private val methodNames: Map<Int, String>
) : BytecodeImplementation() {

    override fun apply(
        mv: MethodVisitor,
        context: Implementation.Context,
        method: MethodDescription
    ): ByteCodeAppender.Size {
        if (firstChars.isEmpty()) {
            mv.visitInsn(Opcodes.ACONST_NULL)
            mv.visitInsn(Opcodes.ARETURN)
            return ByteCodeAppender.Size(1, 2)
        }

        val returnNullLabel = Label()

        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitJumpInsn(Opcodes.IFNULL, returnNullLabel)
        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "length",
            "()I",
            false
        )
        mv.visitJumpInsn(Opcodes.IFEQ, returnNullLabel)

        mv.visitVarInsn(Opcodes.ALOAD, 1)
        mv.visitInsn(Opcodes.ICONST_0)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "java/lang/String",
            "charAt",
            "(I)C",
            false
        )

        val defaultLabel = returnNullLabel
        val caseLabels = firstChars.map { Label() }

        mv.visitLookupSwitchInsn(
            defaultLabel,
            firstChars,
            caseLabels.toTypedArray()
        )

        val ownerInternalName = context.instrumentedType.internalName
        val staticPClassInternalName = "tech/kaffa/portrait/aot/StaticPClass"
        firstChars.forEachIndexed { index, codePoint ->
            mv.visitLabel(caseLabels[index])
            val methodName = methodNames.getValue(codePoint)

            mv.visitVarInsn(Opcodes.ALOAD, 1)
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ownerInternalName,
                methodName,
                "(Ljava/lang/String;)Ltech/kaffa/portrait/aot/StaticPortrait;",
                false
            )
            mv.visitVarInsn(Opcodes.ASTORE, 2)
            mv.visitVarInsn(Opcodes.ALOAD, 2)
            mv.visitJumpInsn(Opcodes.IFNULL, returnNullLabel)

            mv.visitTypeInsn(Opcodes.NEW, staticPClassInternalName)
            mv.visitInsn(Opcodes.DUP)
            mv.visitVarInsn(Opcodes.ALOAD, 2)
            mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                staticPClassInternalName,
                "<init>",
                "(Ltech/kaffa/portrait/aot/StaticPortrait;)V",
                false
            )
            mv.visitInsn(Opcodes.ARETURN)
        }

        mv.visitLabel(returnNullLabel)
        mv.visitInsn(Opcodes.ACONST_NULL)
        mv.visitInsn(Opcodes.ARETURN)

        return ByteCodeAppender.Size(3, 3)
    }
}
