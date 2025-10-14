package kaffa.portrait.e2e

import kaffa.portrait.Portrait
import kaffa.portrait.e2e.fixtures.Calculator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * E2E tests for Portrait proxy generation.
 * These tests run on JVM (via JUnit).
 * To enable TeaVM support, see build.gradle.kts for instructions.
 */
class ProxyTest {

    @Test
    fun testCreateProxy() {
        val pClass = Portrait.of(Calculator::class.java)

        assertNotNull(pClass)
        // Note: PClass doesn't expose isInterface, but we can still create proxies for interfaces
    }

    @Test
    fun testProxyInvocation() {
        val pClass = Portrait.of(Calculator::class.java)

        val proxy = pClass.createProxy { self, method, args ->
            when (method.name) {
                "add" -> {
                    val a = args!![0] as Int
                    val b = args[1] as Int
                    a + b
                }
                "multiply" -> {
                    val a = args!![0] as Int
                    val b = args[1] as Int
                    a * b
                }
                else -> throw UnsupportedOperationException("Unknown method: ${method.name}")
            }
        } as Calculator

        assertEquals(8, proxy.add(5, 3))
        assertEquals(15, proxy.multiply(3, 5))
    }

    @Test
    fun testProxyWithStatefulHandler() {
        val pClass = Portrait.of(Calculator::class.java)
        var callCount = 0

        val proxy = pClass.createProxy { self, method, args ->
            callCount++
            when (method.name) {
                "add" -> {
                    val a = args!![0] as Int
                    val b = args[1] as Int
                    a + b
                }
                "multiply" -> {
                    val a = args!![0] as Int
                    val b = args[1] as Int
                    a * b
                }
                else -> throw UnsupportedOperationException("Unknown method: ${method.name}")
            }
        } as Calculator

        proxy.add(1, 2)
        proxy.multiply(3, 4)

        assertEquals(2, callCount)
    }
}
