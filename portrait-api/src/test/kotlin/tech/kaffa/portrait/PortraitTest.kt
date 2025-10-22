package tech.kaffa.portrait

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PortraitTest {

    @BeforeTest
    fun resetCache() {
        Portrait.clearCache()
    }

    @Test
    fun `Portrait of(Class) returns provider-backed class`() {
        val portrait = Portrait.of(TestClass::class.java)
        assertEquals(TestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait of(KClass) returns provider-backed class`() {
        val portrait = Portrait.of(TestClass::class)
        assertEquals(TestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait of(Class) throws when class is unknown`() {
        assertFailsWith<PortraitNotFoundException> {
            Portrait.of(UnknownTestClass::class.java)
        }
    }

    @Test
    fun `Portrait ofOrNull returns null for unknown class`() {
        val portrait = Portrait.ofOrNull(UnknownTestClass::class.java)
        assertNull(portrait)
    }

    @Test
    fun `Portrait ofOrUnresolved returns sentinel for unknown class`() {
        val portrait = Portrait.ofOrUnresolved(UnknownTestClass::class.java)
        assertTrue(Portrait.isUnresolved(portrait))
        assertEquals(UnknownTestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait ofOrNull returns provider result when available`() {
        val portrait = Portrait.ofOrNull(TestClass::class.java)
        assertNotNull(portrait)
        assertEquals(TestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait ofOrNull with KClass returns null for unknown class`() {
        val portrait = Portrait.ofOrNull(UnknownTestClass::class)
        assertNull(portrait)
    }

    @Test
    fun `Portrait ofOrUnresolved with KClass returns sentinel for unknown class`() {
        val portrait = Portrait.ofOrUnresolved(UnknownTestClass::class)
        assertTrue(Portrait.isUnresolved(portrait))
        assertEquals(UnknownTestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait from delegates to instance type`() {
        val instance = TestClass("value")
        val portrait = Portrait.from(instance)
        assertEquals(TestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait from throws when instance type unknown`() {
        assertFailsWith<PortraitNotFoundException> {
            Portrait.from(UnknownTestClass("value"))
        }
    }

    @Test
    fun `Portrait fromOrNull returns null when instance type unknown`() {
        val portrait = Portrait.fromOrNull(UnknownTestClass("value"))
        assertNull(portrait)
    }

    @Test
    fun `Portrait fromOrUnresolved returns sentinel when instance type unknown`() {
        val portrait = Portrait.fromOrUnresolved(UnknownTestClass("value"))
        assertTrue(Portrait.isUnresolved(portrait))
        assertEquals(UnknownTestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait forName returns provider-backed class`() {
        val portrait = Portrait.forName(TestClass::class.java.name)
        assertEquals(TestClass::class.java.name, portrait.qualifiedName)
    }

    @Test
    fun `Portrait forName throws for unknown class`() {
        assertFailsWith<PortraitNotFoundException> {
            Portrait.forName("unknown.class.Name")
        }
    }

    @Test
    fun `Portrait forNameOrNull returns null for unknown class`() {
        val portrait = Portrait.forNameOrNull("unknown.class.Name")
        assertNull(portrait)
    }

    @Test
    fun `Portrait forNameOrUnresolved returns sentinel for unknown class`() {
        val portrait = Portrait.forNameOrUnresolved("unknown.class.Name")
        assertTrue(Portrait.isUnresolved(portrait))
        assertEquals("unknown.class.Name", portrait.qualifiedName)
    }

    @Test
    fun `Portrait cache returns same instance for repeated lookups`() {
        val first = Portrait.of(TestClass::class.java)
        val second = Portrait.of(TestClass::class.java)
        assertSame(first, second)
    }
}

class UnknownTestClass(private val value: String)

