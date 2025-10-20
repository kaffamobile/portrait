package tech.kaffa.portrait.internal

import kotlin.test.assertNull
import kotlin.test.Test

class InternalPortraitProviderTest {

    private val provider = InternalPortraitProvider()

    @Test
    fun `rejects void array descriptors`() {
        val result = provider.forName<Any>("[V")
        assertNull(result, "void arrays are not valid JVM class descriptors")
    }
}

