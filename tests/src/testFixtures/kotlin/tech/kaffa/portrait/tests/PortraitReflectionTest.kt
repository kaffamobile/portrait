package tech.kaffa.portrait.tests

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.tests.fixtures.AnnotatedTestClass
import tech.kaffa.portrait.tests.fixtures.ExceptionTestClass
import tech.kaffa.portrait.tests.fixtures.MultiConstructorClass
import tech.kaffa.portrait.tests.fixtures.ParameterTestClass
import tech.kaffa.portrait.tests.fixtures.SimpleReflectiveClass
import tech.kaffa.portrait.tests.fixtures.SingletonService
import tech.kaffa.portrait.tests.fixtures.Status
import tech.kaffa.portrait.tests.fixtures.TestClass
import tech.kaffa.portrait.tests.fixtures.TestDataClass
import tech.kaffa.portrait.tests.fixtures.TestInterface
import tech.kaffa.portrait.tests.fixtures.TestSingleton

open class PortraitReflectionTest : PortraitTestSupport() {

    @Test
    fun `Portrait exposes core metadata for String`() {
        val stringPClass = Portrait.forName("java.lang.String")

        assertEquals("String", stringPClass.simpleName)
        assertEquals("java.lang.String", stringPClass.qualifiedName)
        assertTrue(stringPClass.methods.isNotEmpty())
        assertNotNull(stringPClass.getMethod("substring", Portrait.intClass(), Portrait.intClass()))
        assertNull(stringPClass.getMethod("doesNotExist"))
    }

    @Test
    fun `Portrait creates instances via constructors`() {
        val pClass = Portrait.of(TestClass::class)

        val defaultInstance = pClass.createInstance()
        assertEquals("default", defaultInstance.getInternalValue())

        val customInstance = pClass.createInstance("custom")
        assertEquals("custom", customInstance.getInternalValue())
    }

    @Test
    fun `Portrait resolves superclass and interfaces`() {
        val stringClass = Portrait.forName("java.lang.String")
        val objectClass = Portrait.forName("java.lang.Object")

        assertEquals(objectClass.qualifiedName, stringClass.superclass?.qualifiedName)

        val interfaceNames = stringClass.interfaces.map(PClass<*>::simpleName)
        assertTrue("Serializable" in interfaceNames)
        assertTrue("Comparable" in interfaceNames)
        assertTrue("CharSequence" in interfaceNames)
    }

    @Test
    fun `Portrait returns Kotlin object instances`() {
        val singleton = Portrait.of(TestSingleton::class)
        val instance = singleton.objectInstance

        assertNotNull(instance)
        assertEquals("singleton", instance.getSingletonValue())
    }

    @Test
    fun `Portrait finds and invokes methods`() {
        val pClass = Portrait.of(TestClass::class)
        val instance = pClass.createInstance("value")

        val doSomething = pClass.getMethod("doSomething")
        assertNotNull(doSomething)
        assertEquals("did something with value", doSomething.invoke(instance))

        val processValue = pClass.getMethod("processValue", Portrait.intClass())
        assertNotNull(processValue)
        assertEquals(20, processValue.invoke(instance, 10))
    }

    @Test
    fun `Portrait reads annotations`() {
        val annotated = Portrait.of(AnnotatedTestClass::class)
        val classAnnotation = annotated.annotations.single { it.annotationClass.simpleName == "TestAnnotation" }
        assertEquals("class-level", classAnnotation.getStringValue("value"))

        val method = annotated.getMethod("annotatedMethod")
        assertNotNull(method)
        val methodAnnotation = method.annotations.single { it.annotationClass.simpleName == "TestAnnotation" }
        assertEquals(200, methodAnnotation.getIntValue("number"))
    }

    @Test
    fun `Portrait inspects fields`() {
        val dataPClass = Portrait.of(TestDataClass::class)
        val field = dataPClass.getField("name")
        assertNotNull(field)

        val instance = TestDataClass(1, "demo")
        assertEquals("demo", field.get(instance))
    }

    @Test
    fun `Portrait handles enums and sealed types`() {
        val status = Portrait.of(Status::class)
        assertTrue(status.isEnum)
        val constants = status.enumConstants
        assertNotNull(constants)
        assertEquals(4, constants.size)

        val operation = Portrait.of(SimpleReflectiveClass::class)
        assertTrue(operation.methods.any { it.name == "greet" })
    }

    @Test
    fun `Portrait supports varargs and arrays`() {
        val parameterClass = Portrait.of(ParameterTestClass::class)
        val arrayMethod = parameterClass.methods.first { it.name == "arrayParams" }
        val instance = parameterClass.createInstance()
        val result = arrayMethod.invoke(instance, intArrayOf(1, 2, 3), arrayOf("a"))
        assertEquals("arrays", result)
    }

    @Test
    fun `Portrait selects matching constructor overload`() {
        val multiConstructor = Portrait.of(MultiConstructorClass::class)

        val oneArg = multiConstructor.createInstance("first")
        assertEquals("first", oneArg.name)
        assertEquals(0, oneArg.value)

        val twoArgs = multiConstructor.createInstance("second", 5)
        assertEquals(5, twoArgs.value)
        assertNull(twoArgs.optional)

        val threeArgs = multiConstructor.createInstance("third", 7, "maybe")
        assertEquals("maybe", threeArgs.optional)
    }

    @Test
    fun `Portrait surfaces exceptions thrown by invoked methods`() {
        val exceptionClass = Portrait.of(ExceptionTestClass::class)
        val throwsException = exceptionClass.getMethod("throwsException")
        val instance = exceptionClass.createInstance()

        assertFailsWith<RuntimeException> {
            throwsException!!.invoke(instance)
        }
    }

    @Test
    fun `Portrait proxies interfaces`() {
        val interfacePClass = Portrait.of(TestInterface::class)
        val proxy = interfacePClass.createProxy { _, method, args ->
            when (method.name) {
                "doSomething" -> "proxied"
                "processValue" -> (args?.firstOrNull() as? Int)?.times(3)
                "getName" -> "proxy"
                else -> null
            }
        }

        assertEquals("proxied", proxy.doSomething())
        assertEquals(9, proxy.processValue(3))
        assertEquals("proxy", proxy.getName())
    }
}
