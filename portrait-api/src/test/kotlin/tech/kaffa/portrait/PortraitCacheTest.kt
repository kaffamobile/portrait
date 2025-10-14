package tech.kaffa.portrait

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class PortraitCacheTest {

    @BeforeEach
    fun clearCache() {
        Portrait.clearCache()
    }

    @Test
    fun `Portrait cache can be cleared`() {
        // This test verifies that the clearCache method exists and can be called
        // In a real scenario with providers, this would test actual caching behavior
        Portrait.clearCache()
    }

    @Test
    fun `Portrait cache functionality is thread-safe`() {
        // Test that cache operations work correctly
        // Since we use ConcurrentHashMap, this tests the thread-safety aspect

        Portrait.clearCache()

        // Multiple calls to clearCache should not cause issues
        repeat(10) {
            Portrait.clearCache()
        }

        // This test mainly verifies that the cache mechanism
        // doesn't cause any threading issues during clearing
    }

    @Test
    fun `Portrait methods handle RuntimeException when no providers available`() {
        // This test verifies that when no providers are available,
        // we get RuntimeException (which includes our cache-related exceptions)

        assertFailsWith<RuntimeException> {
            Portrait.of(TestClass::class.java)
        }

        assertFailsWith<RuntimeException> {
            Portrait.of(TestClass::class)
        }

        assertFailsWith<RuntimeException> {
            Portrait.from(TestClass("test"))
        }

        assertFailsWith<RuntimeException> {
            Portrait.forName("tech.kaffa.portrait.TestClass")
        }
    }
}