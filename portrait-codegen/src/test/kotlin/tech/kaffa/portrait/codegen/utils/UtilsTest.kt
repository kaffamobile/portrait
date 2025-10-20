package tech.kaffa.portrait.codegen.utils

import io.mockk.every
import io.mockk.mockk
import net.bytebuddy.description.annotation.AnnotationDescription
import net.bytebuddy.description.annotation.AnnotationList
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.method.MethodList
import net.bytebuddy.description.method.ParameterList
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.description.type.TypeList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class UtilsTest {

    @Test
    fun `AnnotationUtils extension functions work correctly`() {
        val mockAnnotationDesc = mockk<AnnotationDescription>()
        val mockAnnotationType = mockk<TypeDescription>()

        every { mockAnnotationDesc.annotationType } returns mockAnnotationType
        every { mockAnnotationType.typeName } returns "com.example.TestAnnotation"
        every { mockAnnotationType.simpleName } returns "TestAnnotation"
        every { mockAnnotationType.declaredMethods } returns MethodList.Empty()

        val annotationEntry = mockAnnotationDesc.toAnnotationEntry()

        assertEquals("com.example.TestAnnotation", annotationEntry.annotationClassName)
        assertEquals("TestAnnotation", annotationEntry.simpleName)
        assertEquals("com.example.TestAnnotation", annotationEntry.qualifiedName)
    }

    @Test
    fun `TypeDescription extensions work correctly`() {
        val mockTypeDesc = mockk<TypeDescription>()

        every { mockTypeDesc.typeName } returns "com.example.TestClass"
        every { mockTypeDesc.represents(Any::class.java) } returns false

        assertFalse(mockTypeDesc.isObjectClass())
        assertEquals("com.example.TestClass", mockTypeDesc.qualifiedNameOrNull())
    }

    @Test
    fun `TypeDescription superclass handling`() {
        val mockTypeDesc = mockk<TypeDescription>()
        val mockSuperClass = mockk<TypeDescription>()
        val mockObjectClass = mockk<TypeDescription>()
        val mockSuperGeneric = mockk<TypeDescription.Generic>()
        val mockObjectGeneric = mockk<TypeDescription.Generic>()

        every { mockSuperClass.asGenericType() } returns mockSuperGeneric
        every { mockSuperGeneric.represents(Any::class.java) } returns false
        every { mockSuperGeneric.typeName } returns "com.example.SuperClass"
        every { mockTypeDesc.superClass } returns mockSuperGeneric

        assertEquals("com.example.SuperClass", mockTypeDesc.superclassNameOrNull())

        // Test with Object superclass (should return null)
        every { mockObjectClass.asGenericType() } returns mockObjectGeneric
        every { mockObjectGeneric.represents(Any::class.java) } returns true
        every { mockTypeDesc.superClass } returns mockObjectGeneric

        assertNull(mockTypeDesc.superclassNameOrNull())
    }

    @Test
    fun `TypeDescription interface names extraction`() {
        val mockTypeDesc = mockk<TypeDescription>()
        val mockInterface1 = mockk<TypeDescription>()
        val mockInterface2 = mockk<TypeDescription>()
        val mockInterfaceList = mockk<TypeList.Generic>()

        every { mockTypeDesc.interfaces } returns mockInterfaceList
        every { mockInterfaceList.asErasures() } returns TypeList.Explicit(
            mockInterface1,
            mockInterface2
        )
        every { mockInterface1.typeName } returns "java.io.Serializable"
        every { mockInterface2.typeName } returns "java.lang.Comparable"

        val interfaceNames = mockTypeDesc.interfaceNames()
        assertEquals(2, interfaceNames.size)
        assertEquals("java.io.Serializable", interfaceNames[0])
        assertEquals("java.lang.Comparable", interfaceNames[1])
    }

    @Test
    fun `MethodDescription parameter type names extraction`() {
        val mockMethodDesc = mockk<MethodDescription>()
        val mockParameterList = mockk<ParameterList<*>>()
        val mockParam1 = mockk<TypeDescription.Generic>()
        val mockParam2 = mockk<TypeDescription.Generic>()

        every { mockMethodDesc.parameters } returns mockParameterList
        every { mockParameterList.asTypeList() } returns TypeList.Generic.Explicit(
            mockParam1,
            mockParam2
        )
        every { mockParam1.typeName } returns "java.lang.String"
        every { mockParam2.typeName } returns "int"
        every { mockParam1.asGenericType() } returns mockParam1
        every { mockParam2.asGenericType() } returns mockParam2

        val parameterTypeNames = mockMethodDesc.parameterTypeNames()
        assertEquals(2, parameterTypeNames.size)
        assertEquals("java.lang.String", parameterTypeNames[0])
        assertEquals("int", parameterTypeNames[1])
    }

    @Test
    fun `MethodDescription return type name extraction`() {
        val mockMethodDesc = mockk<MethodDescription>()
        val mockReturnType = mockk<TypeDescription.Generic>()

        every { mockMethodDesc.returnType } returns mockReturnType
        every { mockReturnType.typeName } returns "java.lang.Object"

        assertEquals("java.lang.Object", mockMethodDesc.returnTypeName())
    }

    @Test
    fun `TypeDescription qualified name handling`() {
        val mockTypeDesc1 = mockk<TypeDescription>()
        val mockTypeDesc2 = mockk<TypeDescription>()

        every { mockTypeDesc1.typeName } returns "com.example.QualifiedClass"
        every { mockTypeDesc2.typeName } returns "SimpleClass"

        assertEquals("com.example.QualifiedClass", mockTypeDesc1.qualifiedNameOrNull())
        assertNull(mockTypeDesc2.qualifiedNameOrNull()) // No package
    }

    @Test
    fun `Extension functions handle empty collections`() {
        val mockTypeDesc = mockk<TypeDescription>()

        every { mockTypeDesc.interfaces } returns TypeList.Generic.Empty()
        every { mockTypeDesc.interfaces.asErasures() } returns TypeList.Empty()
        every { mockTypeDesc.declaredAnnotations } returns AnnotationList.Empty()

        assertEquals(emptyList(), mockTypeDesc.interfaceNames())
        assertEquals(emptyList(), mockTypeDesc.toAnnotationEntries())
    }
}
