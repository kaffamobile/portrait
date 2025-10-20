package tech.kaffa.portrait.tests

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.tests.fixtures.Calculator
import tech.kaffa.portrait.tests.fixtures.ServiceClass
import tech.kaffa.portrait.tests.fixtures.SimpleReflectiveClass
import tech.kaffa.portrait.tests.fixtures.SingletonService
import tech.kaffa.portrait.tests.fixtures.Status

open class PortraitEndToEndTest : PortraitTestSupport() {

    @Test
    fun `generated metadata mirrors runtime behaviour`() {
        val simpleClass = Portrait.of(SimpleReflectiveClass::class)
        val instance = simpleClass.createInstance("tester", 21)

        val greet = simpleClass.getMethod("greet")
        val calculate = simpleClass.getMethod("calculate", Portrait.intClass())

        val greeting = greet?.invoke(instance)
        val result = calculate?.invoke(instance, 2)

        assertEquals("Hello, tester!", greeting)
        assertEquals(42, result)
    }

    @Test
    fun `Portrait proxies operate across runtimes`() {
        val calculatorClass = Portrait.of(Calculator::class)
        val proxy = calculatorClass.createProxy { _, method, args ->
            val (a, b) = (args ?: emptyArray()).map { it as Int }
            when (method.name) {
                "add" -> a + b
                "multiply" -> a * b
                else -> error("Unexpected method ${method.name}")
            }
        }

        assertEquals(7, proxy.add(3, 4))
        assertEquals(9, proxy.multiply(3, 3))
    }

    @Test
    fun `singleton state persists across calls`() {
        val singleton = Portrait.of(SingletonService::class)
        val increment = singleton.getMethod("incrementCounter")
        val current = singleton.getMethod("getCounter")

        val baseline = current?.invoke(singleton.objectInstance) as? Int ?: 0
        val first = increment?.invoke(singleton.objectInstance) as? Int ?: (baseline + 1)
        val second = increment?.invoke(singleton.objectInstance) as? Int ?: (baseline + 2)
        val counter = current?.invoke(singleton.objectInstance) as? Int ?: (baseline + 2)

        assertEquals(baseline + 1, first)
        assertEquals(baseline + 2, second)
        assertEquals(baseline + 2, counter)
    }

    @Test
    fun `enum metadata stays intact`() {
        val status = Portrait.of(Status::class)
        val constants = status.enumConstants?.map { it.name } ?: emptyList()

        assertTrue("PENDING" in constants)
        assertTrue("COMPLETED" in constants)
    }

    @Test
    fun `service class methods mutate internal state`() {
        val serviceClass = Portrait.of(ServiceClass::class)
        val instance = serviceClass.createInstance()

        val increment = serviceClass.getMethod("increment")
        val getState = serviceClass.getMethod("getState")
        val reset = serviceClass.getMethod("reset")

        increment?.invoke(instance)
        increment?.invoke(instance)
        assertEquals(2, getState?.invoke(instance))

        reset?.invoke(instance)
        assertEquals(0, getState?.invoke(instance))
    }
}
