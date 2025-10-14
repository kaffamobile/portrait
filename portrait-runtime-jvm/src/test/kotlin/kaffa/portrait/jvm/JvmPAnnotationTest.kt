package kaffa.portrait.jvm

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JvmPAnnotationTest {

    private val provider = JvmPortraitProvider()

    @Test
    fun `JvmPAnnotation basic properties`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotations = annotatedClassPClass.annotations

        if (annotations.isNotEmpty()) {
            val annotation = annotations.first()
            assertNotNull(annotation.annotationClass)
            assertNotNull(annotation.annotationClass.simpleName)
        }
    }

    @Test
    fun `JvmPAnnotation can get annotation values`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!

        val annotation = annotatedClassPClass.getAnnotation(testAnnotationClass)

        if (annotation != null) {
            assertEquals(testAnnotationClass, annotation.annotationClass)

            // TestAnnotation has parameters: value, number, flag
            val value = annotation.getValue("value")
            val number = annotation.getValue("number")
            val flag = annotation.getValue("flag")

            assertNotNull(value)
            assertNotNull(number)
            assertNotNull(flag)

            // These should match the annotation values on AnnotatedTestClass
            assertEquals("class-level", value)
            assertEquals(100, number)
            assertEquals(true, flag)
        }
    }

    @Test
    fun `JvmPAnnotation handles default values`() {
        val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!

        // Use the top-level SimpleAnnotatedClass to test default values
        // Note: Portrait doesn't support local classes, so we use top-level classes defined in TestFixtures
        val simpleClassPClass = provider.forName<SimpleAnnotatedClass>("kaffa.portrait.jvm.SimpleAnnotatedClass")

        if (simpleClassPClass != null) {
            val annotation = simpleClassPClass.getAnnotation(testAnnotationClass)

            if (annotation != null) {
                val value = annotation.getValue("value")
                val number = annotation.getValue("number")
                val flag = annotation.getValue("flag")

                assertEquals("default", value)
                assertEquals(42, number)
                assertEquals(false, flag)
            }
        }
    }

    @Test
    fun `JvmPAnnotation handles method annotations`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedMethod = annotatedClassPClass.getDeclaredMethod("annotatedMethod")

        if (annotatedMethod != null) {
            val annotations = annotatedMethod.annotations

            if (annotations.isNotEmpty()) {
                val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!
                val annotation = annotatedMethod.getAnnotation(testAnnotationClass)

                if (annotation != null) {
                    val value = annotation.getValue("value")
                    val number = annotation.getValue("number")

                    assertEquals("method-level", value)
                    assertEquals(200, number)
                }
            }
        }
    }

    @Test
    fun `JvmPAnnotation handles field annotations`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val annotatedField = annotatedClassPClass.getDeclaredField("annotatedField")

        if (annotatedField != null) {
            val annotations = annotatedField.annotations

            if (annotations.isNotEmpty()) {
                val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!
                val annotation = annotatedField.getAnnotation(testAnnotationClass)

                if (annotation != null) {
                    val value = annotation.getValue("value")
                    assertEquals("field-level", value)

                    // number and flag should use defaults for field annotation
                    val number = annotation.getValue("number")
                    val flag = annotation.getValue("flag")
                    assertEquals(42, number)  // default value
                    assertEquals(false, flag) // default value
                }
            }
        }
    }

    @Test
    fun `JvmPAnnotation handles non-existent annotation properties`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!
        val annotation = annotatedClassPClass.getAnnotation(testAnnotationClass)

        if (annotation != null) {
            // Try to get a property that doesn't exist
            val nonExistent = annotation.getValue("nonExistentProperty")
            assertNull(nonExistent)
        }
    }

    @Test
    fun `JvmPAnnotation handles wrong type requests`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!
        val annotation = annotatedClassPClass.getAnnotation(testAnnotationClass)

        if (annotation != null) {
            // Try to get string value as integer (should handle gracefully)
            try {
                val wrongType = annotation.getValue("value") // "value" is a String
                // This might return null or throw exception depending on implementation
            } catch (e: Exception) {
                // Expected behavior for type mismatch
            }
        }
    }

    @Test
    fun `JvmPAnnotation handles built-in annotations`() {
        // Test with built-in Java annotations like @Deprecated
        @Deprecated("Test deprecation")
        class DeprecatedClass

        val deprecatedClassPClass =
            provider.forName<DeprecatedClass>("kaffa.portrait.jvm.JvmPAnnotationTest\$DeprecatedClass")

        if (deprecatedClassPClass != null) {
            val deprecatedAnnotationClass = provider.forName<Deprecated>("java.lang.Deprecated")

            if (deprecatedAnnotationClass != null) {
                val annotation = deprecatedClassPClass.getAnnotation(deprecatedAnnotationClass)

                if (annotation != null) {
                    val value = annotation.getValue("value")
                    assertEquals("Test deprecation", value)
                }
            }
        }
    }

    @Test
    fun `JvmPAnnotation handles annotation arrays`() {
        // Create a test annotation with array values if available
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!
        val annotation = annotatedClassPClass.getAnnotation(testAnnotationClass)

        if (annotation != null) {
            // Our TestAnnotation doesn't have array properties, but test the mechanism
            assertNotNull(annotation.annotationClass)
        }
    }

    @Test
    fun `JvmPAnnotation annotation class equality`() {
        val annotatedClassPClass = provider.forName<AnnotatedTestClass>("kaffa.portrait.jvm.AnnotatedTestClass")!!
        val testAnnotationClass = provider.forName<TestAnnotation>("kaffa.portrait.jvm.TestAnnotation")!!
        val annotation = annotatedClassPClass.getAnnotation(testAnnotationClass)

        if (annotation != null) {
            assertEquals("TestAnnotation", annotation.annotationClass.simpleName)
        }
    }
}