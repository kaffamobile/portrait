package tech.kaffa.portrait.aot.meta.serde

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StringPoolTest {

    @Test
    fun `StringPool stores and retrieves strings`() {
        val pool = StringPool()

        val string1 = "test string"
        val string2 = "another string"

        val id1 = pool.intern(string1)
        val id2 = pool.intern(string2)

        assertEquals(string1, pool.getString(id1))
        assertEquals(string2, pool.getString(id2))
        assertNotEquals(id1, id2)
    }

    @Test
    fun `StringPool deduplicates identical strings`() {
        val pool = StringPool()

        val string = "duplicate string"
        val id1 = pool.intern(string)
        val id2 = pool.intern(string)

        assertEquals(id1, id2, "Same string should return same ID")
        assertEquals(string, pool.getString(id1))
        assertEquals(string, pool.getString(id2))
    }

    @Test
    fun `StringPool handles empty strings`() {
        val pool = StringPool()

        val emptyString = ""
        val id = pool.intern(emptyString)

        assertEquals(emptyString, pool.getString(id))
    }

    @Test
    fun `StringPool handles empty string as null substitute`() {
        val pool = StringPool()

        // Since StringPool doesn't support null, we use empty string as substitute
        val id = pool.intern("")
        assertEquals("", pool.getString(id))
    }

    @Test
    fun `StringPool handles different empty-like strings`() {
        val pool = StringPool()

        val emptyId = pool.intern("")
        val spaceId = pool.intern(" ")

        assertNotEquals(emptyId, spaceId)
        assertEquals("", pool.getString(emptyId))
        assertEquals(" ", pool.getString(spaceId))
    }

    @Test
    fun `StringPool handles special characters`() {
        val pool = StringPool()

        val specialString = "Special chars: !@#$%^&*()[]{}|\\:;\"'<>,.?/~`"
        val id = pool.intern(specialString)

        assertEquals(specialString, pool.getString(id))
    }

    @Test
    fun `StringPool handles unicode characters`() {
        val pool = StringPool()

        val unicodeString = "Unicode: 🔥 💯 ⭐ 🚀 中文 العربية Русский"
        val id = pool.intern(unicodeString)

        assertEquals(unicodeString, pool.getString(id))
    }

    @Test
    fun `StringPool handles very long strings`() {
        val pool = StringPool()

        val longString = "a".repeat(10000)
        val id = pool.intern(longString)

        assertEquals(longString, pool.getString(id))
    }

    @Test
    fun `StringPool maintains consistent IDs across multiple operations`() {
        val pool = StringPool()

        val strings = listOf("first", "second", "third", "first", "second")
        val ids = strings.map { pool.intern(it) }

        // First occurrences should have unique IDs
        assertNotEquals(ids[0], ids[1])
        assertNotEquals(ids[1], ids[2])

        // Duplicate strings should have same IDs
        assertEquals(ids[0], ids[3]) // "first"
        assertEquals(ids[1], ids[4]) // "second"
    }

    @Test
    fun `StringPool serialization compatibility`() {
        val pool = StringPool()

        val testStrings = listOf(
            "java.lang.String",
            "tech.kaffa.portrait.jvm.TestClass",
            "getDeclaredMethod",
            "parameterTypes",
            "",
            "very.long.qualified.class.name.with.many.packages"
        )

        val ids = testStrings.map { pool.intern(it) }

        // Verify all strings can be retrieved correctly
        for ((index, originalString) in testStrings.withIndex()) {
            assertEquals(originalString, pool.getString(ids[index]))
        }
    }

    @Test
    fun `StringPool size tracking`() {
        val pool = StringPool()

        assertEquals(0, pool.size())

        pool.intern("first")
        assertEquals(1, pool.size())

        pool.intern("second")
        assertEquals(2, pool.size())

        pool.intern("first") // duplicate
        assertEquals(2, pool.size()) // should not increase
    }

    @Test
    fun `StringPool iteration over strings`() {
        val pool = StringPool()

        val originalStrings = setOf("one", "two", "three", "")
        originalStrings.forEach { pool.intern(it) }

        val retrievedStrings = mutableSetOf<String>()
        for (i in 0 until pool.size()) {
            retrievedStrings.add(pool.getString(i))
        }

        assertEquals(originalStrings, retrievedStrings)
    }
}
