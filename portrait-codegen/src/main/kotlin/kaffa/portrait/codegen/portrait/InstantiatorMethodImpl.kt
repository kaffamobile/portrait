package kaffa.portrait.codegen.portrait

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes

class InstantiatorMethodImpl(
    superType: TypeDescription,
    constructors: List<IndexedValue<MethodDescription>>
) : TableSwitchingImplementation<MethodDescription>(superType, constructors) {

    override fun generateItemImplementation(
        item: MethodDescription,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ) {
        val declaringType = item.declaringType.asErasure()

        methodVisitor.visitTypeInsn(Opcodes.NEW, declaringType.internalName)
        methodVisitor.visitInsn(Opcodes.DUP)

        for ((paramIndex, parameter) in item.parameters.withIndex()) {
            loadArrayElement(
                methodVisitor,
                INSTANCE_PARAM,
                paramIndex,
                parameter.type
            )
        }

        methodVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            declaringType.internalName,
            item.internalName,
            item.descriptor,
            false
        )

        // Return the created instance
        methodVisitor.visitInsn(Opcodes.ARETURN)
    }

    override fun getIndexOutOfBoundsMessage(): String =
        "Invalid constructor index for ${superType.name}"

    override fun calculateMaxStack(): Int {
        // Conservative estimate: object + dup + max parameters + overhead
        val maxParams = items.maxOfOrNull { it.value.parameters.size } ?: 0
        return maxOf(CONSERVATIVE_MINIMUM, maxParams + INSTANCE_AND_DUP_OVERHEAD + BOXING_OVERHEAD)
    }
}
