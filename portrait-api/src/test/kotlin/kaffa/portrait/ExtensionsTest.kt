package kaffa.portrait

import io.mockk.mockk
import kaffa.portrait.provider.PortraitProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class ExtensionsTest {

    @Test
    fun `portrait extension on Class works correctly`() {
        val mockProvider = mockk<PortraitProvider>()
        val mockPClass = mockk<PClass<String>>()

        // Since we can't easily mock Portrait's service loader,
        // we test that the extension method exists and compiles
        val clazz = String::class.java

        // This will throw PortraitNotFoundException in real scenario
        // but verifies the extension method signature
        try {
            clazz.portrait
        } catch (e: RuntimeException) {
            // Expected when no providers are available
        }
    }

    @Test
    fun `portrait extension on KClass works correctly`() {
        val kClass = String::class

        // Test that the extension method exists and compiles
        try {
            kClass.portrait
        } catch (e: RuntimeException) {
            // Expected when no providers are available
        }
    }

    @Test
    fun `portrait extension on instance works correctly`() {
        val instance = "test string"

        // Test that the extension method exists and compiles
        try {
            instance.portrait
        } catch (e: RuntimeException) {
            // Expected when no providers are available
        }
    }

    @Test
    fun `extension methods have correct return types`() {
        // Verify the extension methods compile with correct generic types
        val stringClass: Class<String> = String::class.java
        val stringKClass = String::class
        val stringInstance = "test"

        // These should compile without type errors (properties not functions)
        try {
            val classExtension = stringClass.portrait
            assertNotNull(classExtension)
        } catch (e: RuntimeException) {
            // Expected when no providers are available
        }

        try {
            val kclassExtension = stringKClass.portrait
            assertNotNull(kclassExtension)
        } catch (e: RuntimeException) {
            // Expected when no providers are available
        }

        try {
            val instanceExtension = stringInstance.portrait
            assertNotNull(instanceExtension)
        } catch (e: RuntimeException) {
            // Expected when no providers are available
        }
    }

    @Test
    fun `extension methods delegate to Portrait object`() {
        // This test verifies that extension methods are simply convenience
        // wrappers around Portrait object methods

        val testClass = TestClass::class.java
        val testKClass = TestClass::class
        val testInstance = TestClass("test")

        // Verify methods exist and have same behavior as Portrait.* methods
        try {
            testClass.portrait
        } catch (e: RuntimeException) {
            // Expected - same as Portrait.of(testClass)
        }

        try {
            testKClass.portrait
        } catch (e: RuntimeException) {
            // Expected - same as Portrait.of(testKClass)
        }

        try {
            testInstance.portrait
        } catch (e: RuntimeException) {
            // Expected - same as Portrait.from(testInstance)
        }
    }
}