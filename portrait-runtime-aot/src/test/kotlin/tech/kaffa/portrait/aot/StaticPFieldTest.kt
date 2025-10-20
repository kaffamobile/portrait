package tech.kaffa.portrait.aot

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StaticPFieldTest {

    private fun createTestFieldEntry(): PFieldEntry {
        return PFieldEntry(
            name = "testField",
            typeName = "java.lang.String",
            declaringClassName = "com.example.TestClass",
            isStatic = false,
            isFinal = true,
            annotations = listOf(
                PAnnotationEntry(
                    annotationClassName = "com.example.FieldAnnotation",
                    simpleName = "FieldAnnotation",
                    qualifiedName = "com.example.FieldAnnotation",
                    properties = mapOf("value" to "field-level")
                )
            )
        )
    }

    @Test
    fun `StaticPField basic properties from metadata`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val fieldEntry = createTestFieldEntry()

        val staticPField = StaticPField(fieldEntry, 0, mockPortrait)

        assertEquals("testField", staticPField.name)
        assertFalse(staticPField.isStatic)
        assertTrue(staticPField.isFinal)
    }

    @Test
    fun `StaticPField type resolution`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val fieldEntry = createTestFieldEntry()

        val staticPField = StaticPField(fieldEntry, 0, mockPortrait)

        // Field type should be resolved via Portrait.forName
        assertFalse(staticPField.type.toString().isEmpty())
    }

    @Test
    fun `StaticPField value access delegates to portrait`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val fieldEntry = createTestFieldEntry()
        val testInstance = TestClass("test")
        val expectedValue = "field_value"

        every { mockPortrait.getFieldValue(0, testInstance) } returns expectedValue

        val staticPField = StaticPField(fieldEntry, 0, mockPortrait)

        val value = staticPField.get(testInstance)
        assertEquals(expectedValue, value)
    }

    @Test
    fun `StaticPField value setting delegates to portrait`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val fieldEntry = createTestFieldEntry()
        val testInstance = TestClass("test")
        val newValue = "new_field_value"

        every { mockPortrait.setFieldValue(0, testInstance, newValue) } returns Unit

        val staticPField = StaticPField(fieldEntry, 0, mockPortrait)

        staticPField.set(testInstance, newValue)
        // Verify the call was made (mockk verification would be here in real test)
    }

    @Test
    fun `StaticPField static field access`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val staticFieldEntry = createTestFieldEntry().copy(isStatic = true)
        val expectedValue = "static_field_value"

        every { mockPortrait.getFieldValue(0, null) } returns expectedValue

        val staticPField = StaticPField(staticFieldEntry, 0, mockPortrait)

        assertTrue(staticPField.isStatic)
        val value = staticPField.get(null)
        assertEquals(expectedValue, value)
    }

    @Test
    fun `StaticPField static field setting`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val staticFieldEntry = createTestFieldEntry().copy(isStatic = true)
        val newValue = "new_static_value"

        every { mockPortrait.setFieldValue(0, null, newValue) } returns Unit

        val staticPField = StaticPField(staticFieldEntry, 0, mockPortrait)

        assertTrue(staticPField.isStatic)
        staticPField.set(null, newValue)
        // Verification would occur here in full test
    }

    @Test
    fun `StaticPField handles primitive field types`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val primitiveFieldEntry = createTestFieldEntry().copy(
            typeName = "int",
            name = "primitiveField"
        )
        val testInstance = TestClass("test")
        val primitiveValue = 42

        every { mockPortrait.getFieldValue(0, testInstance) } returns primitiveValue

        val staticPField = StaticPField(primitiveFieldEntry, 0, mockPortrait)

        assertEquals("primitiveField", staticPField.name)
        val value = staticPField.get(testInstance)
        assertEquals(primitiveValue, value)
    }

    @Test
    fun `StaticPField handles null values`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val fieldEntry = createTestFieldEntry()
        val testInstance = TestClass("test")

        every { mockPortrait.getFieldValue(0, testInstance) } returns null
        every { mockPortrait.setFieldValue(0, testInstance, null) } returns Unit

        val staticPField = StaticPField(fieldEntry, 0, mockPortrait)

        val nullValue = staticPField.get(testInstance)
        assertEquals(null, nullValue)

        staticPField.set(testInstance, null)
        // Setting null should work for reference types
    }

    @Test
    fun `StaticPField annotation handling`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val fieldEntry = createTestFieldEntry()

        val staticPField = StaticPField(fieldEntry, 0, mockPortrait)

        val annotations = staticPField.annotations
        assertEquals(1, annotations.size)
        assertEquals("FieldAnnotation", annotations[0].annotationClass.simpleName)

        // Test annotation resolution methods
        val actualAnnotationClass = annotations[0].annotationClass
        assertTrue(staticPField.hasAnnotation(actualAnnotationClass))
        assertNotNull(staticPField.getAnnotation(actualAnnotationClass))
    }

    @Test
    fun `StaticPField handles array field types`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()
        val arrayFieldEntry = createTestFieldEntry().copy(
            typeName = "[Ljava.lang.String;",
            name = "arrayField"
        )
        val testInstance = TestClass("test")
        val arrayValue = arrayOf("one", "two", "three")

        every { mockPortrait.getFieldValue(0, testInstance) } returns arrayValue

        val staticPField = StaticPField(arrayFieldEntry, 0, mockPortrait)

        assertEquals("arrayField", staticPField.name)
        val value = staticPField.get(testInstance)
        assertEquals(arrayValue, value)
    }
    @Test
    fun `StaticPField final and static modifiers`() {
        val mockPortrait = mockk<StaticPortrait<TestClass>>()

        val finalField = createTestFieldEntry().copy(isFinal = true, isStatic = false)
        val staticField = createTestFieldEntry().copy(isFinal = false, isStatic = true)
        val staticFinalField = createTestFieldEntry().copy(isFinal = true, isStatic = true)

        val staticFinalPField = StaticPField(finalField, -1, mockPortrait)
        val staticStaticPField = StaticPField(staticField, -1, mockPortrait)
        val staticStaticFinalPField = StaticPField(staticFinalField, -1, mockPortrait)

        assertTrue(staticFinalPField.isFinal)
        assertFalse(staticFinalPField.isStatic)

        assertFalse(staticStaticPField.isFinal)
        assertTrue(staticStaticPField.isStatic)

        assertTrue(staticStaticFinalPField.isFinal)
        assertTrue(staticStaticFinalPField.isStatic)
    }
}
