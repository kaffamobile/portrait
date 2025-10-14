package tech.kaffa.portrait.codegen.portrait

import net.bytebuddy.description.field.FieldDescription
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender
import net.bytebuddy.jar.asm.Label
import net.bytebuddy.jar.asm.MethodVisitor
import net.bytebuddy.jar.asm.Opcodes
import net.bytebuddy.jar.asm.Type
import tech.kaffa.portrait.codegen.utils.BytecodeImplementation

/**
 * Abstract base class for implementations that use table switching for efficient dispatch
 * based on an index parameter.
 */
abstract class TableSwitchingImplementation<T>(
    protected val superType: TypeDescription,
    protected val items: List<IndexedValue<T>>
) : BytecodeImplementation() {

    companion object {
        const val INDEX_PARAM = 1
        const val INSTANCE_PARAM = 2
        const val ARGS_ARRAY_PARAM = 3
        const val VALUE_PARAM = 3

        // Stack calculation constants
        const val BASE_STACK_OVERHEAD = 4
        const val INSTANCE_AND_DUP_OVERHEAD = 2
        const val BOXING_OVERHEAD = 1
        const val CONSERVATIVE_MINIMUM = 6
    }

    override fun apply(
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    ): ByteCodeAppender.Size {

        with(methodVisitor) {
            // Load index parameter (first parameter)
            visitVarInsn(Opcodes.ILOAD, 1)

            // Table switch for item selection
            val labels = Array(items.size) { Label() }
            val defaultLabel = Label()

            visitTableSwitchInsn(0, items.size - 1, defaultLabel, *labels)

            // Generate code for each item
            for ((index, item) in items.withIndex()) {
                visitLabel(labels[index])

                // Generate specific implementation for this item
                generateItemImplementation(
                    item.value,
                    this,
                    implementationContext,
                    instrumentedMethod
                )

                // No need to jump to end label since each case returns directly
            }

            // Default case - throw IndexOutOfBoundsException
            visitLabel(defaultLabel)
            generateIndexOutOfBoundsException(this)
        }

        return ByteCodeAppender.Size(calculateMaxStack(), instrumentedMethod.parameters.size + 1)
    }

    /**
     * Generate the implementation for a specific item at the given index
     */
    protected abstract fun generateItemImplementation(
        item: T,
        methodVisitor: MethodVisitor,
        implementationContext: Implementation.Context,
        instrumentedMethod: MethodDescription
    )

    /**
     * Calculate the maximum stack size needed for this implementation
     */
    protected abstract fun calculateMaxStack(): Int

    /**
     * Get the error message for IndexOutOfBoundsException
     */
    protected abstract fun getIndexOutOfBoundsMessage(): String

    /**
     * Load instance parameter and cast to correct type
     */
    @Suppress("SameParameterValue")
    protected fun loadAndCastInstance(
        mv: MethodVisitor,
        paramIndex: Int,
        targetType: TypeDescription = superType
    ) {
        mv.visitVarInsn(Opcodes.ALOAD, paramIndex)
        mv.visitTypeInsn(Opcodes.CHECKCAST, targetType.internalName)
    }

    /**
     * Box primitive type if needed using pure ASM
     */
    protected fun boxIfNeeded(mv: MethodVisitor, type: TypeDescription.Generic) {
        if (!type.isPrimitive) return

        val asmType = Type.getType(type.asErasure().descriptor)
        val wrapperType = getWrapperType(asmType)

        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            wrapperType.internalName,
            "valueOf",
            "(${asmType.descriptor})${wrapperType.descriptor}",
            false
        )
    }

    /**
     * Unbox primitive type if needed using pure ASM
     */
    protected fun unboxIfNeeded(mv: MethodVisitor, type: TypeDescription.Generic) {
        if (!type.isPrimitive) return

        val asmType = Type.getType(type.asErasure().descriptor)
        val wrapperType = getWrapperType(asmType)
        val unboxMethod = getUnboxMethod(asmType)

        mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperType.internalName)
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            wrapperType.internalName,
            unboxMethod,
            "()${asmType.descriptor}",
            false
        )
    }

    /**
     * Load array element with unboxing if needed
     */
    protected fun loadArrayElement(
        mv: MethodVisitor,
        arrayParamIndex: Int,
        elementIndex: Int,
        elementType: TypeDescription.Generic
    ) {
        mv.visitVarInsn(Opcodes.ALOAD, arrayParamIndex)
        mv.visitLdcInsn(elementIndex)
        mv.visitInsn(Opcodes.AALOAD)
        unboxIfNeeded(mv, elementType)
        if (!elementType.isPrimitive) {
            mv.visitTypeInsn(Opcodes.CHECKCAST, elementType.asErasure().internalName)
        }
    }

    /**
     * Create field access (read or write) with proper instance loading
     */
    protected fun accessField(
        mv: MethodVisitor,
        field: FieldDescription,
        read: Boolean
    ) {
        val opcode = when {
            field.isStatic && read -> Opcodes.GETSTATIC
            field.isStatic && !read -> Opcodes.PUTSTATIC
            !field.isStatic && read -> Opcodes.GETFIELD
            else -> Opcodes.PUTFIELD
        }

        if (!field.isStatic) {
            loadAndCastInstance(mv, INSTANCE_PARAM, field.declaringType.asErasure())
        }

        mv.visitFieldInsn(
            opcode,
            field.declaringType.asErasure().internalName,
            field.name,
            field.type.asErasure().descriptor
        )
    }

    /**
     * Invoke method with proper instruction
     */
    protected fun invokeMethod(mv: MethodVisitor, method: MethodDescription) {
        val owner = method.declaringType.asErasure()

        val opcode = when {
            method.isStatic -> Opcodes.INVOKESTATIC
            method.isConstructor -> Opcodes.INVOKESPECIAL
            method.isPrivate -> Opcodes.INVOKESPECIAL
            owner.isInterface -> Opcodes.INVOKEINTERFACE
            else -> Opcodes.INVOKEVIRTUAL
        }

        mv.visitMethodInsn(
            opcode,
            owner.internalName,
            method.internalName,
            method.descriptor,
            owner.isInterface
        )
    }

    /**
     * Get wrapper type for primitive
     */
    private fun getWrapperType(primitiveType: Type): Type = when (primitiveType.sort) {
        Type.BOOLEAN -> Type.getType(java.lang.Boolean::class.java)
        Type.BYTE -> Type.getType(java.lang.Byte::class.java)
        Type.CHAR -> Type.getType(java.lang.Character::class.java)
        Type.SHORT -> Type.getType(java.lang.Short::class.java)
        Type.INT -> Type.getType(java.lang.Integer::class.java)
        Type.LONG -> Type.getType(java.lang.Long::class.java)
        Type.FLOAT -> Type.getType(java.lang.Float::class.java)
        Type.DOUBLE -> Type.getType(java.lang.Double::class.java)
        else -> throw IllegalArgumentException("Not a primitive type: $primitiveType")
    }

    /**
     * Get unbox method name for primitive
     */
    private fun getUnboxMethod(primitiveType: Type): String = when (primitiveType.sort) {
        Type.BOOLEAN -> "booleanValue"
        Type.BYTE -> "byteValue"
        Type.CHAR -> "charValue"
        Type.SHORT -> "shortValue"
        Type.INT -> "intValue"
        Type.LONG -> "longValue"
        Type.FLOAT -> "floatValue"
        Type.DOUBLE -> "doubleValue"
        else -> throw IllegalArgumentException("Not a primitive type: $primitiveType")
    }

    private fun generateIndexOutOfBoundsException(mv: MethodVisitor) {
        // NEW IndexOutOfBoundsException
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/IndexOutOfBoundsException")
        // DUP
        mv.visitInsn(Opcodes.DUP)
        // LDC message
        mv.visitLdcInsn(getIndexOutOfBoundsMessage())
        // INVOKESPECIAL IndexOutOfBoundsException.<init>(String)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/lang/IndexOutOfBoundsException",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        )
        // ATHROW
        mv.visitInsn(Opcodes.ATHROW)
    }
}
