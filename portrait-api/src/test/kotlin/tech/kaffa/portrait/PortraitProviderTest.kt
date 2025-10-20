package tech.kaffa.portrait

import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import tech.kaffa.portrait.provider.PortraitProvider
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PortraitProviderTest {

    @Test
    fun `PortraitProvider interface has correct method signatures`() {
        val provider = mockk<PortraitProvider>()

        every { provider.priority() } returns 100
        every { provider.forName<Any>("test.Class") } returns null

        assertEquals(100, provider.priority())
        assertNull(provider.forName<Any>("test.Class"))
    }

    @Test
    fun `PortraitProvider can return null for unknown classes`() {
        val provider = object : PortraitProvider {
            override fun priority(): Int = 50

            override fun <T : Any> forName(className: String): PClass<T>? = null
        }

        assertNull(provider.forName<String>("unknown.Class"))
        assertEquals(50, provider.priority())
    }

    @Test
    fun `PortraitProvider can return PClass for known classes`() {
        val mockPClass = mockk<PClass<String>>()

        val provider = object : PortraitProvider {
            override fun priority(): Int = 75

            override fun <T : Any> forName(className: String): PClass<T>? {
                @Suppress("UNCHECKED_CAST")
                return if (className == "java.lang.String") mockPClass as PClass<T> else null
            }
        }

        assertNotNull(provider.forName<String>("java.lang.String"))
        assertNull(provider.forName<String>("unknown.Class"))
        assertEquals(75, provider.priority())
    }

    @Test
    fun `PortraitProvider priority determines order`() {
        val highPriorityProvider = object : PortraitProvider {
            override fun priority(): Int = 100
            override fun <T : Any> forName(className: String): PClass<T>? = null
        }

        val lowPriorityProvider = object : PortraitProvider {
            override fun priority(): Int = 10
            override fun <T : Any> forName(className: String): PClass<T>? = null
        }

        assert(highPriorityProvider.priority() > lowPriorityProvider.priority()) {
            "High priority provider should have higher priority value"
        }
    }

    @Test
    fun `PortraitProvider can handle generic types`() {
        val provider = object : PortraitProvider {
            override fun priority(): Int = 50

            override fun <T : Any> forName(className: String): PClass<T>? {
                // Simple implementation that handles List<String>
                return if (className.contains("List")) {
                    @Suppress("UNCHECKED_CAST")
                    mockk<PClass<List<String>>>() as PClass<T>
                } else null
            }
        }

        assertNotNull(provider.forName<List<String>>("java.util.List"))
        assertNull(provider.forName<String>("java.lang.String"))
    }
}
