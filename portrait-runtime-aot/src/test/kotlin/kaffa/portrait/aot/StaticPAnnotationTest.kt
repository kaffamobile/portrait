package kaffa.portrait.aot

import kaffa.portrait.aot.meta.PAnnotationEntry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StaticPAnnotationTest {

    private fun createTestAnnotationEntry(): PAnnotationEntry {
        return PAnnotationEntry(
            annotationClassName = "kaffa.portrait.aot.TestAnnotation",
            simpleName = "TestAnnotation",
            qualifiedName = "kaffa.portrait.aot.TestAnnotation",
            properties = mapOf(
                "value" to "test_value",
                "number" to 42,
                "flag" to true,
                "array" to arrayOf("one", "two", "three"),
                "nested" to mapOf("key" to "nested_value")
            )
        )
    }

    @Test
    fun `StaticPAnnotation basic properties from metadata`() {
        val annotationEntry = createTestAnnotationEntry()
        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        // The annotation class should be resolvable since TestAnnotation exists in the test classpath
        assertNotNull(staticPAnnotation.annotationClass)
        assertEquals("TestAnnotation", staticPAnnotation.annotationClass.simpleName)
        assertEquals("kaffa.portrait.aot.TestAnnotation", staticPAnnotation.annotationClass.qualifiedName)
    }

    @Test
    fun `StaticPAnnotation can get string property values`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        val stringValue = staticPAnnotation.getValue("value") as? String
        assertEquals("test_value", stringValue)
    }

    @Test
    fun `StaticPAnnotation can get numeric property values`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        val numberValue = staticPAnnotation.getValue("number") as? Int
        assertEquals(42, numberValue)
    }

    @Test
    fun `StaticPAnnotation can get boolean property values`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        val booleanValue = staticPAnnotation.getValue("flag") as? Boolean
        assertEquals(true, booleanValue)
    }

    @Test
    fun `StaticPAnnotation can get array property values`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        @Suppress("UNCHECKED_CAST")
        val arrayValue = staticPAnnotation.getValue("array") as? Array<String>
        assertNotNull(arrayValue)
        assertEquals(3, arrayValue.size)
        assertEquals("one", arrayValue[0])
        assertEquals("two", arrayValue[1])
        assertEquals("three", arrayValue[2])
    }

    @Test
    fun `StaticPAnnotation handles non-existent properties`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        val nonExistentValue = staticPAnnotation.getValue("nonExistentProperty") as? String
        assertNull(nonExistentValue)
    }

    @Test
    fun `StaticPAnnotation handles type mismatches gracefully`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        // Try to get a string value as an integer
        val mismatchedValue = staticPAnnotation.getValue("value") as? Int
        // This should return null or handle the type mismatch gracefully
        // The exact behavior depends on implementation
        assertNull(mismatchedValue)
    }

    @Test
    fun `StaticPAnnotation handles null property values`() {
        val annotationEntryWithNull = PAnnotationEntry(
            annotationClassName = "kaffa.portrait.aot.TestAnnotation",
            simpleName = "TestAnnotation",
            qualifiedName = "kaffa.portrait.aot.TestAnnotation",
            properties = mapOf(
                "nullValue" to null,
                "normalValue" to "not_null"
            )
        )

        val staticPAnnotation = StaticPAnnotation(annotationEntryWithNull)

        val nullValue = staticPAnnotation.getValue("nullValue") as? String
        assertNull(nullValue)

        val normalValue = staticPAnnotation.getValue("normalValue") as? String
        assertEquals("not_null", normalValue)
    }

    @Test
    fun `StaticPAnnotation handles empty properties map`() {
        val emptyAnnotationEntry = PAnnotationEntry(
            annotationClassName = "kaffa.portrait.aot.TestAnnotation",
            simpleName = "TestAnnotation",
            qualifiedName = "kaffa.portrait.aot.TestAnnotation",
            properties = emptyMap()
        )

        val staticPAnnotation = StaticPAnnotation(emptyAnnotationEntry)

        val anyValue = staticPAnnotation.getValue("anyProperty") as? String
        assertNull(anyValue)
    }

    @Test
    fun `StaticPAnnotation handles complex nested values`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        @Suppress("UNCHECKED_CAST")
        val nestedValue = staticPAnnotation.getValue("nested") as? Map<String, Any>
        assertNotNull(nestedValue)
        assertEquals("nested_value", nestedValue["key"])
    }

    @Test
    fun `StaticPAnnotation handles annotation default values`() {
        // Test annotation with only some properties set, others should use defaults
        val partialAnnotationEntry = PAnnotationEntry(
            annotationClassName = "kaffa.portrait.aot.TestAnnotation",
            simpleName = "TestAnnotation",
            qualifiedName = "kaffa.portrait.aot.TestAnnotation",
            properties = mapOf(
                "value" to "custom_value"
                // number and flag not specified, should use defaults if available
            )
        )

        val staticPAnnotation = StaticPAnnotation(partialAnnotationEntry)

        val customValue = staticPAnnotation.getValue("value") as? String
        assertEquals("custom_value", customValue)

        val defaultNumber = staticPAnnotation.getValue("number") as? Int
        // Should be null since not provided and no default handling in static metadata
        assertNull(defaultNumber)
    }

    @Test
    fun `StaticPAnnotation type casting with generics`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        // Test getting values with different generic types
        val stringValue = staticPAnnotation.getValue("value") as? String
        val anyValue = staticPAnnotation.getValue("value")
        val objectValue = staticPAnnotation.getValue("value")

        assertEquals("test_value", stringValue)
        assertEquals("test_value", anyValue)
        assertEquals("test_value", objectValue)
    }

    @Test
    fun `StaticPAnnotation property key case sensitivity`() {
        val annotationEntry = createTestAnnotationEntry()

        val staticPAnnotation = StaticPAnnotation(annotationEntry)

        // Properties should be case-sensitive
        val correctCase = staticPAnnotation.getValue("value") as? String
        val wrongCase = staticPAnnotation.getValue("VALUE") as? String

        assertEquals("test_value", correctCase)
        assertNull(wrongCase) // Case mismatch should return null
    }
}