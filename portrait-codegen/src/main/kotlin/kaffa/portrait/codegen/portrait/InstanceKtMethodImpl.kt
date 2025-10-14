package kaffa.portrait.codegen.portrait

import kaffa.portrait.codegen.utils.BytecodeImplementation
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes

class InstanceKtMethodImpl(private val superType: TypeDescription) : BytecodeImplementation() {
    override fun apply(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ): ByteCodeAppender.Size {

        val instanceField = superType.declaredFields
            .firstOrNull { it.name == "INSTANCE" && it.isStatic }
            ?: throw IllegalStateException("INSTANCE field not found for Kotlin object: ${superType.name}")

        methodVisitor.visitFieldInsn(
            Opcodes.GETSTATIC,
            instanceField.declaringType.asErasure().internalName,
            instanceField.name,
            instanceField.type.asErasure().descriptor
        )

        methodVisitor.visitInsn(Opcodes.ARETURN)

        return ByteCodeAppender.Size(1, instrumentedMethod.parameters.size + 1)
    }
}
