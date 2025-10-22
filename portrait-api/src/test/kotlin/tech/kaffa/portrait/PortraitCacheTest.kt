package tech.kaffa.portrait

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PortraitCacheTest {

    @BeforeTest
    fun setUp() {
        Portrait.clearCache()
    }

    @AfterTest
    fun tearDown() {
        Portrait.clearCache()
    }

    @Test
    fun `clearCache removes cached entries`() {
        val first = Portrait.of(TestClass::class.java)
        val second = Portrait.of(TestClass::class.java)
        assertSame(first, second)

        Portrait.clearCache()

        val refreshed = Portrait.of(TestClass::class.java)
        assertSame(first, refreshed)
    }

    @Test
    fun `cache does not retain unresolved lookups`() {
        val unresolvedFirst = Portrait.ofOrUnresolved(UnknownTestClass::class.java)
        assertTrue(Portrait.isUnresolved(unresolvedFirst))

        Portrait.clearCache()

        val unresolvedSecond = Portrait.ofOrUnresolved(UnknownTestClass::class.java)
        assertTrue(Portrait.isUnresolved(unresolvedSecond))
    }

    @Test
    fun `clearCache is idempotent`() {
        repeat(10) {
            Portrait.clearCache()
        }
    }
}

