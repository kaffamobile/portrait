package tech.kaffa.portrait.jvm

import org.junit.jupiter.api.Test
import tech.kaffa.portrait.Portrait
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmPortraitProviderTest {

    private val provider = JvmPortraitProvider()

    @Test
    fun `JvmPortraitProvider has appropriate priority`() {
        val priority = provider.priority()
        assertTrue(priority > 0, "JVM provider should have positive priority")
        // JVM provider typically has lower priority than AOT
        assertTrue(priority == 100, "JVM provider should have lower priority than AOT providers")
    }

    @Test
    fun `JvmPortraitProvider can create PClass for known Java classes`() {
        val stringPClass = provider.forName<String>("java.lang.String")

        assertNotNull(stringPClass, "Should create PClass for String")
        assertEquals("String", stringPClass.simpleName)
        assertEquals("java.lang.String", stringPClass.qualifiedName)
    }

    @Test
    fun `JvmPortraitProvider can create PClass for test fixtures`() {
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")

        assertNotNull(testClassPClass, "Should create PClass for TestClass")
        assertEquals("TestClass", testClassPClass.simpleName)
        assertEquals("tech.kaffa.portrait.jvm.TestClass", testClassPClass.qualifiedName)
    }

    @Test
    fun `JvmPortraitProvider returns null for non-existent classes`() {
        val nonExistentPClass = provider.forName<Any>("com.example.NonExistentClass")

        assertNull(nonExistentPClass, "Should return null for non-existent class")
    }

    @Test
    fun `JvmPortraitProvider can handle primitive wrapper classes`() {
        val integerPClass = Portrait.intClass()
        val booleanPClass = Portrait.booleanClass()

        assertNotNull(integerPClass, "Should create PClass for Integer")
        assertNotNull(booleanPClass, "Should create PClass for Boolean")

        assertEquals("int", integerPClass.simpleName)
        assertEquals("boolean", booleanPClass.simpleName)
    }

    @Test
    fun `JvmPortraitProvider can handle array types`() {
        val stringArrayPClass = provider.forName<Array<String>>("[Ljava.lang.String;")
        val intArrayPClass = provider.forName<IntArray>("[I")

        assertNotNull(stringArrayPClass, "Should create PClass for String array")
        assertNotNull(intArrayPClass, "Should create PClass for int array")
    }

    @Test
    fun `JvmPortraitProvider can handle inner classes`() {
        // Test with Map.Entry which is a well-known inner interface
        val entryPClass = provider.forName<Map.Entry<*, *>>("java.util.Map\$Entry")

        assertNotNull(entryPClass, "Should create PClass for Map.Entry")
        assertEquals("Entry", entryPClass.simpleName)
    }

    @Test
    fun `JvmPortraitProvider handles ClassNotFoundException gracefully`() {
        val invalidPClass = provider.forName<Any>("invalid..class..name")

        assertNull(invalidPClass, "Should return null for invalid class names")
    }

    @Test
    fun `JvmPortraitProvider can create PClass for interfaces`() {
        val listPClass = provider.forName<List<*>>("java.util.List")
        val runnablePClass = provider.forName<Runnable>("java.lang.Runnable")

        assertNotNull(listPClass, "Should create PClass for List interface")
        assertNotNull(runnablePClass, "Should create PClass for Runnable interface")

//        assertTrue(listPClass.isInterface, "List should be recognized as interface")
//        assertTrue(runnablePClass.isInterface, "Runnable should be recognized as interface")
    }

    @Test
    fun `JvmPortraitProvider can create PClass for abstract classes`() {
        val numberPClass = provider.forName<Number>("java.lang.Number")

        assertNotNull(numberPClass, "Should create PClass for Number abstract class")
        assertTrue(numberPClass.isAbstract, "Number should be recognized as abstract")
    }
}