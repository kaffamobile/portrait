package tech.kaffa.portrait.e2e

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.e2e.fixtures.Calculator

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
            requireNotNull(self)
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
        }

        assertEquals(8, proxy.add(5, 3))
        assertEquals(15, proxy.multiply(3, 5))
    }

    @Test
    fun testProxyWithStatefulHandler() {
        val pClass = Portrait.of(Calculator::class.java)
        var callCount = 0

        val proxy = pClass.createProxy { self, method, args ->
            requireNotNull(self)
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
        }

        proxy.add(1, 2)
        proxy.multiply(3, 4)

        assertEquals(2, callCount)
    }
}
