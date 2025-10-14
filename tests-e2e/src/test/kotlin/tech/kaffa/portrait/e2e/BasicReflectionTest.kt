package tech.kaffa.portrait.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.e2e.fixtures.Addition
import tech.kaffa.portrait.e2e.fixtures.FieldTestClass
import tech.kaffa.portrait.e2e.fixtures.MultiConstructorClass
import tech.kaffa.portrait.e2e.fixtures.Multiplication
import tech.kaffa.portrait.e2e.fixtures.NullableTestClass
import tech.kaffa.portrait.e2e.fixtures.ServiceClass
import tech.kaffa.portrait.e2e.fixtures.SimpleReflectiveClass
import tech.kaffa.portrait.e2e.fixtures.SingletonService
import tech.kaffa.portrait.e2e.fixtures.Status
import tech.kaffa.portrait.internal.UnresolvedPClass
import tech.kaffa.portrait.portrait

/**
 * Basic E2E tests for Portrait reflection API.
 * These tests run on JVM (via JUnit).
 * To enable TeaVM support, see build.gradle.kts for instructions.
 */
class BasicReflectionTest {

    @Test
    fun testSimpleClassReflection() {
        val pClass = Portrait.of(SimpleReflectiveClass::class.java)

        assertNotNull(pClass)
        assertEquals("SimpleReflectiveClass", pClass.simpleName)
        assertEquals("tech.kaffa.portrait.e2e.fixtures.SimpleReflectiveClass", pClass.qualifiedName)
    }

    @Test
    fun testCreateInstance() {
        val pClass = Portrait.of(SimpleReflectiveClass::class.java)
        val constructor = pClass.getConstructor(String::class.java.portrait, Int::class.java.portrait)

        assertNotNull(constructor)
        val instance = constructor?.call("Test", 100) as SimpleReflectiveClass

        assertEquals("Test", instance.name)
        assertEquals(100, instance.value)
    }

    @Test
    fun testMethodInvocation() {
        val instance = SimpleReflectiveClass("World", 10)
        val pClass = Portrait.from(instance)

        val greetMethod = pClass.getDeclaredMethod("greet")
        assertNotNull(greetMethod)

        val result = greetMethod?.invoke(instance) as String
        assertEquals("Hello, World!", result)
    }

    @Test
    fun testMethodWithParameters() {
        val instance = SimpleReflectiveClass("Test", 5)
        val pClass = Portrait.from(instance)

        val calculateMethod = pClass.getDeclaredMethod("calculate", Int::class.java.portrait)
        assertNotNull(calculateMethod)

        val result = calculateMethod?.invoke(instance, 3) as Int
        assertEquals(15, result)
    }

    @Test
    fun testFieldAccess() {
        val instance = FieldTestClass()
        val pClass = Portrait.from(instance)

        val publicField = pClass.getDeclaredField("publicField")
        assertNotNull(publicField)

        val value = publicField!!.get(instance) as String
        assertEquals("public", value)

        publicField.set(instance, "modified")
        assertEquals("modified", instance.publicField)
    }

    @Test
    fun testSealedClassHierarchy() {
        val addPClass = Portrait.of(Addition::class.java)
        val mulPClass = Portrait.of(Multiplication::class.java)

        assertNotNull(addPClass)
        assertNotNull(mulPClass)

        val addInstance = Addition(5, 3)
        val mulInstance = Multiplication(4, 7)

        assertEquals(8, addInstance.execute())
        assertEquals(28, mulInstance.execute())
    }

    @Test
    fun testObjectInstance() {
        val pClass = Portrait.of(SingletonService::class.java)

        assertNotNull(pClass)
        assertNotNull(pClass.objectInstance)

        val instance = pClass.objectInstance as SingletonService
        val counter1 = instance.incrementCounter()
        val counter2 = instance.incrementCounter()

        assertTrue(counter2 > counter1)
    }

    @Test
    fun testEnumReflection() {
        val pClass = Portrait.of(Status::class.java)

        assertNotNull(pClass)
        assertEquals("Status", pClass.simpleName)
    }

    @Test
    fun testMultipleConstructors() {
        val pClass = Portrait.of(MultiConstructorClass::class.java)

        val constructor1 = pClass.getConstructor(String::class.java.portrait)
        assertNotNull(constructor1)

        val instance1 = constructor1?.call("Test1") as MultiConstructorClass
        assertEquals("Test1", instance1.name)
        assertEquals(0, instance1.value)

        val constructor2 = pClass.getConstructor(String::class.java.portrait, Int::class.java.portrait)
        assertNotNull(constructor2)

        val instance2 = constructor2?.call("Test2", 42) as MultiConstructorClass
        assertEquals("Test2", instance2.name)
        assertEquals(42, instance2.value)
    }

    @Test
    fun testNullableTypes() {
        val instance1 = NullableTestClass("required", null)
        val instance2 = NullableTestClass("required", "optional")

        assertEquals("default", instance1.getOptionalOrDefault("default"))
        assertEquals("optional", instance2.getOptionalOrDefault("default"))
    }

    @Test
    fun testServiceClass() {
        val pClass = Portrait.of(ServiceClass::class.java)
        val instance = pClass.createInstance()

        val incrementMethod = pClass.getDeclaredMethod("increment")
        val getStateMethod = pClass.getDeclaredMethod("getState")

        assertNotNull(incrementMethod)
        assertNotNull(getStateMethod)

        val result1 = incrementMethod?.invoke(instance) as Int
        assertEquals(1, result1)

        val state = getStateMethod?.invoke(instance) as Int
        assertEquals(1, state)
    }

    @Test
    fun testForName() {
        val pClass = Portrait.forName("tech.kaffa.portrait.e2e.fixtures.SimpleReflectiveClass")

        assertNotNull(pClass)
        assertEquals("SimpleReflectiveClass", pClass.simpleName)
    }

    @Test
    fun testForNameOrUnresolved() {
        val validClass = Portrait.forNameOrUnresolved("tech.kaffa.portrait.e2e.fixtures.SimpleReflectiveClass")
        assertNotNull(validClass)
        assertFalse(validClass is UnresolvedPClass)

        val invalidClass = Portrait.forNameOrUnresolved("non.existent.Class")
        assertNotNull(invalidClass)
        assertTrue(invalidClass is UnresolvedPClass)
    }
}
