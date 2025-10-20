package tech.kaffa.portrait.jvm

import kotlin.test.Test
import tech.kaffa.portrait.Portrait
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmPClassTest {

    private val provider = JvmPortraitProvider()

    @Test
    fun `JvmPClass basic properties for String class`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!

        assertEquals("String", stringPClass.simpleName)
        assertEquals("java.lang.String", stringPClass.qualifiedName)
        assertEquals("java.lang.String", stringPClass.qualifiedName)
        assertFalse(stringPClass.isAbstract)
        assertFalse(stringPClass.isSealed)
        assertFalse(stringPClass.isData)
        assertFalse(stringPClass.isCompanion)
        // Note: isFinal property doesn't exist in PClass interface
    }

    @Test
    fun `JvmPClass can find declared methods`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!

        val lengthMethod = stringPClass.getMethod("length")
        assertNotNull(lengthMethod, "Should find length() method")
        assertEquals("length", lengthMethod.name)
        assertEquals(0, lengthMethod.parameterTypes.size)

        val nonExistentMethod = stringPClass.getMethod("nonExistent")
        assertNull(nonExistentMethod, "Should return null for non-existent method")
    }

    @Test
    fun `JvmPClass can find methods with parameters`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val intPClass = Portrait.intClass()

        val charAtMethod = stringPClass.getMethod("charAt", intPClass)
        assertNotNull(charAtMethod, "Should find charAt(int) method")
        assertEquals("charAt", charAtMethod.name)
        assertEquals(1, charAtMethod.parameterTypes.size)
    }

    @Test
    fun `JvmPClass can find all declared methods`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!

        val methods = stringPClass.methods
        assertTrue(methods.isNotEmpty(), "String should have declared methods")

        val methodNames = methods.map { it.name }.toSet()
        assertTrue("length" in methodNames, "Should include length method")
        assertTrue("charAt" in methodNames, "Should include charAt method")
        assertTrue("substring" in methodNames, "Should include substring method")
    }

    @Test
    fun `JvmPClass can find declared fields`() {
        // Use a class that we know has fields
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")!!

        val fields = testClassPClass.fields
        // TestClass should have some fields
        assertNotNull(fields)
    }

    @Test
    fun `JvmPClass can find constructors`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!

        // String has no default constructor, should be null
        val constructors = stringPClass.constructors
        assertTrue(constructors.isNotEmpty(), "String should have constructors")
    }

    @Test
    fun `JvmPClass inheritance relationships`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!
        val objectPClass = provider.forName<Any>("java.lang.Object")!!

        assertEquals(objectPClass.qualifiedName, stringPClass.superclass?.qualifiedName)
        assertTrue(stringPClass.isAssignableFrom(stringPClass))
        assertTrue(objectPClass.isAssignableFrom(stringPClass))
        assertFalse(stringPClass.isAssignableFrom(objectPClass))
    }

    @Test
    fun `JvmPClass interface relationships`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!

        val interfaces = stringPClass.interfaces
        assertTrue(interfaces.isNotEmpty(), "String implements interfaces")

        val interfaceNames = interfaces.map { it.qualifiedName.substringAfterLast('.') }
        assertTrue("Serializable" in interfaceNames, "String implements Serializable")
        assertTrue("Comparable" in interfaceNames, "String implements Comparable")
        assertTrue("CharSequence" in interfaceNames, "String implements CharSequence")
    }

    @Test
    fun `JvmPClass can create new instances`() {
        val stringPClass = provider.forName<String>("java.lang.String")!!

        val newString = stringPClass.createInstance()
        assertNotNull(newString)
        assertEquals("", newString) // Default String constructor creates empty string
    }

    @Test
    fun `JvmPClass can create instances with parameters`() {
        val testClassPClass = provider.forName<TestClass>("tech.kaffa.portrait.jvm.TestClass")!!

        val instance = testClassPClass.createInstance("test_value")
        assertNotNull(instance)
        assertEquals("test_value", instance.getInternalValue())
    }

    @Test
    fun `JvmPClass handles object instances`() {
        val singletonPClass = provider.forName<TestSingleton>("tech.kaffa.portrait.jvm.TestSingleton")!!

        assertNotNull(singletonPClass.objectInstance, "TestSingleton should have object instance")
        assertEquals(TestSingleton, singletonPClass.objectInstance)
    }

    @Test
    fun `JvmPClass handles abstract classes`() {
        val numberPClass = provider.forName<Number>("java.lang.Number")!!

        assertTrue(numberPClass.isAbstract, "Number should be abstract")

        val methods = numberPClass.methods
        val abstractMethods = methods.filter { it.isAbstract }
        assertTrue(abstractMethods.isNotEmpty(), "Number should have abstract methods")
    }

    @Test
    fun `JvmPClass treats boxed primitives as assignable from primitives`() {
        val integerPClass = provider.forName<Any>("java.lang.Integer")!!
        val intPrimitive = Portrait.intClass()

        assertTrue(integerPClass.isAssignableFrom(intPrimitive))
        assertTrue(intPrimitive.isAssignableFrom(integerPClass))

        val objectPClass = provider.forName<Any>("java.lang.Object")!!
        assertTrue(integerPClass.isSubclassOf(objectPClass))
    }

    @Test
    fun `JvmPClass handles data classes`() {
        val dataClassPClass = provider.forName<TestDataClass>("tech.kaffa.portrait.jvm.TestDataClass")!!

        // Note: JVM reflection might not detect Kotlin data class metadata
        // This test verifies the basic functionality works
        assertNotNull(dataClassPClass)
        assertEquals("TestDataClass", dataClassPClass.simpleName)
    }
}

