package tech.kaffa.portrait.codegen.portrait

import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes

class FieldSetterMethodImpl(
    superType: TypeDescription,
    fields: List<IndexedValue<FieldDescription>>
) : TableSwitchingImplementation<FieldDescription>(superType, fields) {

    override fun generateItemImplementation(
        item: FieldDescription,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ) {
        if (!item.isStatic) {
            loadAndCastInstance(methodVisitor, INSTANCE_PARAM, item.declaringType.asErasure())
        }

        methodVisitor.visitVarInsn(Opcodes.ALOAD, VALUE_PARAM)

        if (item.type.isPrimitive) {
            unboxIfNeeded(methodVisitor, item.type)
        } else {
            methodVisitor.visitTypeInsn(
                Opcodes.CHECKCAST,
                item.type.asErasure().internalName
            )
        }

        val opcode = if (item.isStatic) Opcodes.PUTSTATIC else Opcodes.PUTFIELD
        methodVisitor.visitFieldInsn(
            opcode,
            item.declaringType.asErasure().internalName,
            item.name,
            item.type.asErasure().descriptor
        )

        // Return void
        methodVisitor.visitInsn(Opcodes.RETURN)
    }

    override fun getIndexOutOfBoundsMessage(): String =
        "Invalid field index for ${superType.name}"

    override fun calculateMaxStack(): Int {
        // Conservative estimate: instance + value + unboxing overhead
        return BASE_STACK_OVERHEAD
    }
}
