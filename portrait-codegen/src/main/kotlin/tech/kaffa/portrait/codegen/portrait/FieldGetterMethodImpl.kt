package tech.kaffa.portrait.codegen.portrait

import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes

class FieldGetterMethodImpl(
    superType: TypeDescription,
    fields: List<IndexedValue<FieldDescription>>
) : TableSwitchingImplementation<FieldDescription>(superType, fields) {

    override fun generateItemImplementation(
        item: FieldDescription,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ) {
        accessField(methodVisitor, item, read = true)

        boxIfNeeded(methodVisitor, item.type)

        // Return the result
        methodVisitor.visitInsn(Opcodes.ARETURN)
    }

    override fun getIndexOutOfBoundsMessage(): String =
        "Invalid field index for ${superType.name}"

    override fun calculateMaxStack(): Int {
        // Conservative estimate: instance + field value + boxing overhead
        return BASE_STACK_OVERHEAD
    }
}
