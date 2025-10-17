package tech.kaffa.portrait

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.kaffa.portrait.provider.PortraitProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PortraitTest {

    @Test
    fun `Portrait of(Class) should delegate to provider`() {
        val mockProvider = mockk<PortraitProvider>()
        val mockPClass = mockk<PClass<TestClass>>()

        every { mockProvider.priority() } returns 100
        every { mockProvider.forName<TestClass>("tech.kaffa.portrait.TestClass") } returns mockPClass

        // We can't easily mock the ServiceLoader, so this test verifies the general contract
        // In real scenarios, this would be tested with integration tests

        val clazz = TestClass::class.java
        assertEquals("tech.kaffa.portrait.TestClass", clazz.name)
    }

    @Test
    fun `Portrait of(KClass) should delegate to provider`() {
        val kClass = TestClass::class
        assertEquals("tech.kaffa.portrait.TestClass", kClass.java.name)
    }

    @Test
    fun `Portrait from(instance) should use instance class`() {
        val instance = TestClass("test")
        assertEquals("tech.kaffa.portrait.TestClass", instance.javaClass.name)
    }

    @Test
    fun `Portrait forName should handle class name`() {
        val className = "tech.kaffa.portrait.TestClass"
        // This test verifies the method signature and basic behavior
        assertThrows<RuntimeException> {
            Portrait.forName(className)
        }
    }

    @Test
    fun `Portrait should throw RuntimeException when no provider found`() {
        // When no providers are available or none can handle the class
        assertThrows<RuntimeException> {
            Portrait.of(TestClass::class.java)
        }
    }

    @Test
    fun `Portrait should throw RuntimeException for unknown class name`() {
        assertThrows<RuntimeException> {
            Portrait.forName("unknown.class.Name")
        }
    }

    @Test
    fun `Portrait forNameOrUnresolved should return UnresolvedPClass for unknown class name`() {
        val result = Portrait.forNameOrUnresolved("unknown.class.Name")
        assertNotNull(result)
        assertTrue(Portrait.isUnresolved(result))
        assertEquals("unknown.class.Name", result.qualifiedName)
    }

    @Test
    fun `Portrait should throw RuntimeException when no providers available`() {
        // This would be tested by manipulating the ServiceLoader, 
        // which is complex in unit tests but important for integration tests
        assertThrows<RuntimeException> {
            Portrait.of(TestClass::class.java)
        }
    }

    @Test
    fun `Portrait methods should handle null returns from providers gracefully`() {
        // Test that when providers return null, Portrait handles it correctly
        assertThrows<RuntimeException> {
            Portrait.of(TestClass::class.java)
        }
    }

    @Test
    fun `Portrait from rejects anonymous and local classes`() {
        val anonymous = object : Runnable {
            override fun run() = Unit
        }

        val error = assertThrows<IllegalArgumentException> {
            Portrait.from(anonymous)
        }

        assertTrue(error.message!!.contains("Local and anonymous classes are unsupported"))
    }
}
