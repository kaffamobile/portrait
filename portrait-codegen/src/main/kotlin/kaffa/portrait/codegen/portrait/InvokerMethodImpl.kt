package kaffa.portrait.codegen.portrait

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes

class InvokerMethodImpl(
    superType: TypeDescription,
    methods: List<IndexedValue<MethodDescription>>
) : TableSwitchingImplementation<MethodDescription>(superType, methods) {

    override fun generateItemImplementation(
        item: MethodDescription,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ) {
        // Load instance if not static using helper method
        if (!item.isStatic) {
            loadAndCastInstance(methodVisitor, INSTANCE_PARAM, item.declaringType.asErasure())
        }

        // Load arguments from array using helper method
        for ((paramIndex, parameter) in item.parameters.withIndex()) {
            loadArrayElement(
                methodVisitor,
                ARGS_ARRAY_PARAM,
                paramIndex,
                parameter.type
            )
        }

        invokeMethod(methodVisitor, item)

        // Handle return value
        if (item.returnType.represents(Void.TYPE)) {
            methodVisitor.visitInsn(Opcodes.ACONST_NULL)
        } else {
            boxIfNeeded(methodVisitor, item.returnType)
        }

        // Return the result (always Object or null)
        methodVisitor.visitInsn(Opcodes.ARETURN)
    }

    override fun getIndexOutOfBoundsMessage(): String =
        "Invalid method index for ${superType.name}"

    override fun calculateMaxStack(): Int {
        // Conservative estimate: instance + max parameters + boxing/unboxing overhead
        val maxParams = items.maxOfOrNull { it.value.parameters.size } ?: 0
        return maxOf(CONSERVATIVE_MINIMUM, maxParams + BASE_STACK_OVERHEAD)
    }
}
