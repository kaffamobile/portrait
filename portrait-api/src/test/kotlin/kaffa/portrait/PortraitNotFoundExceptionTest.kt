package kaffa.portrait

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PortraitNotFoundExceptionTest {

    @Test
    fun `PortraitNotFoundException can be created with message`() {
        val message = "Test exception message"
        val exception = PortraitNotFoundException(message)

        assertEquals(message, exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `PortraitNotFoundException can be created with message and cause`() {
        val message = "Test exception message"
        val cause = RuntimeException("Underlying cause")
        val exception = PortraitNotFoundException(message, cause)

        assertEquals(message, exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `PortraitNotFoundException can be created with just cause`() {
        val cause = RuntimeException("Underlying cause")
        val exception = PortraitNotFoundException("Test exception", cause)

        assertNotNull(exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `PortraitNotFoundException is a RuntimeException`() {
        val exception = PortraitNotFoundException("test")

        assert(exception is RuntimeException) {
            "PortraitNotFoundException should extend RuntimeException"
        }
    }

    @Test
    fun `PortraitNotFoundException can be thrown and caught`() {
        try {
            throw PortraitNotFoundException("Test message")
        } catch (e: PortraitNotFoundException) {
            assertEquals("Test message", e.message)
        }
    }

    @Test
    fun `PortraitNotFoundException maintains stack trace`() {
        val exception = PortraitNotFoundException("Test message")

        assertNotNull(exception.stackTrace)
        assert(exception.stackTrace.isNotEmpty()) {
            "Exception should have stack trace"
        }
    }
}