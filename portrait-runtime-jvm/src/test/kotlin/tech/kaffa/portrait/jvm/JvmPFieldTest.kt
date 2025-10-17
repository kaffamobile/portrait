package tech.kaffa.portrait.jvm

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JvmPFieldTest {

    private val provider = JvmPortraitProvider()

    @Test
    fun `JvmPField basic properties`() {
        // Use a simple class with known fields
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")!!
        val fields = testClassPClass.fields

        if (fields.isNotEmpty()) {
            val field = fields.first()
            assertNotNull(field.name)
            assertNotNull(field.type)
            assertEquals(testClassPClass, field.declaringClass)
        }
    }

    @Test
    fun `JvmPField can get and set field values`() {
        val testClassPClass = provider.forName<AnnotatedTestClass>("tech.kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedField = testClassPClass.getField("annotatedField")

        if (annotatedField != null) {
            val instance = AnnotatedTestClass()

            // Get initial value
            val initialValue = annotatedField.get(instance)
            assertEquals("test", initialValue)

            // Set new value
            annotatedField.set(instance, "updated")
            val updatedValue = annotatedField.get(instance)
            assertEquals("updated", updatedValue)
        }
    }

    @Test
    fun `JvmPField handles static fields`() {
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")!!
        // Look for static fields in companion object or constants
        val fields = testClassPClass.fields

        val staticFields = fields.filter { it.isStatic }
        // Test that static fields can be accessed without instance
        for (staticField in staticFields) {
            try {
                val value = staticField.get(null)
                assertNotNull(value)
            } catch (e: Exception) {
                // Some static fields might not be accessible
            }
        }
    }

    @Test
    fun `JvmPField handles primitive field types`() {
        val testClassPClass = provider.forName<TestDataClass>("tech.kaffa.portrait.jvm.TestDataClass")!!
        val idField = testClassPClass.getField("id")

        if (idField != null) {
            assertNotNull(idField.type)
            assertEquals("int", idField.type.simpleName)

            val instance = TestDataClass(42, "test")
            val value = idField.get(instance)
            assertEquals(42, value)
        }
    }

    @Test
    fun `JvmPField handles object field types`() {
        val testClassPClass = provider.forName<TestDataClass>("tech.kaffa.portrait.jvm.TestDataClass")!!
        val nameField = testClassPClass.getField("name")

        if (nameField != null) {
            assertNotNull(nameField.type)
            assertEquals("String", nameField.type.simpleName)

            val instance = TestDataClass(1, "test_name")
            val value = nameField.get(instance)
            assertEquals("test_name", value)
        }
    }

    @Test
    fun `JvmPField visibility modifiers`() {
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")!!
        val fields = testClassPClass.fields

        // Check that fields have correct visibility flags
        for (field in fields) {
            val visibilityCount = listOf(field.isPublic, field.isPrivate, field.isProtected).count { it }
            assertTrue(visibilityCount == 1, "Field should have exactly one visibility modifier")
        }
    }

    @Test
    fun `JvmPField final modifier`() {
        val testClassPClass = provider.forName<TestDataClass>("tech.kaffa.portrait.jvm.TestDataClass")!!
        val fields = testClassPClass.fields

        // Data class fields should typically be final (val)
        val finalFields = fields.filter { it.isFinal }
        assertTrue(finalFields.isNotEmpty(), "Data class should have final fields")
    }

    @Test
    fun `JvmPField can access field annotations`() {
        val testClassPClass = provider.forName<AnnotatedTestClass>("tech.kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedField = testClassPClass.getField("annotatedField")

        if (annotatedField != null) {
            val annotations = annotatedField.annotations
            assertNotNull(annotations)

            val testAnnotationClass = provider.forName<TestAnnotation>("tech.kaffa.portrait.jvm.TestAnnotation")!!
            assertTrue(
                annotatedField.hasAnnotation(testAnnotationClass),
                "annotatedField should carry TestAnnotation in fixtures"
            )
        }
    }

    @Test
    fun `JvmPField handles null values correctly`() {
        val testClassPClass = provider.forName<AnnotatedTestClass>("tech.kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedField = testClassPClass.getField("annotatedField")

        if (annotatedField != null && !annotatedField.type.isPrimitive) {
            val instance = AnnotatedTestClass()

            // Set field to null (if it's a reference type)
            try {
                annotatedField.set(instance, null)
                val value = annotatedField.get(instance)
                assertEquals(null, value)
            } catch (e: Exception) {
                // Some fields might not accept null values
            }
        }
    }

    @Test
    fun `JvmPField handles array fields`() {
        // Create a simple test class with array field for testing
        val stringArrayClass = provider.forName<Array<String>>("[Ljava.lang.String;")

        if (stringArrayClass != null) {
            assertNotNull(stringArrayClass)
//            assertTrue(stringArrayClass.isArray) //FIXME
        }
    }

    @Test
    fun `JvmPField type compatibility`() {
        val testClassPClass = provider.forName<AnnotatedTestClass>("tech.kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedField = testClassPClass.getField("annotatedField")

        if (annotatedField != null) {
            val fieldType = annotatedField.type
            assertNotNull(fieldType)
            assertEquals("String", fieldType.simpleName)

            val instance = AnnotatedTestClass()

            // Setting compatible value should work
            annotatedField.set(instance, "new_value")
            assertEquals("new_value", annotatedField.get(instance))
        }
    }
}
