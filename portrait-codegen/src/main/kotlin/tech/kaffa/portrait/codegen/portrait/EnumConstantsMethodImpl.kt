package tech.kaffa.portrait.codegen.portrait

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import tech.kaffa.portrait.codegen.utils.BytecodeImplementation

class EnumConstantsMethodImpl(
    private val enumType: TypeDescription
) : BytecodeImplementation() {

    override fun apply(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ): ByteCodeAppender.Size {

        val valuesMethod = enumType.declaredMethods.firstOrNull {
            it.name == "values" && it.parameters.size == 0 && it.isStatic
        } ?: throw IllegalStateException("No enum values() method found for ${enumType.name}")

        methodVisitor.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            enumType.internalName,
            valuesMethod.internalName,
            valuesMethod.descriptor,
            false
        )

        methodVisitor.visitInsn(Opcodes.ARETURN)

        return ByteCodeAppender.Size(1, instrumentedMethod.parameters.size + 1)
    }
}
