package tech.kaffa.portrait.internal

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.kaffa.portrait.Portrait

class ArrayPClassTest {

    @BeforeEach
    fun clearCache() {
        Portrait.clearCache()
    }

    @Test
    fun `reference arrays respect covariance`() {
        val objectArray = Portrait.forName("[Ljava.lang.Object;")
        val stringArray = Portrait.forName("[Ljava.lang.String;")

        assertTrue(
            objectArray.isAssignableFrom(stringArray),
            "Object[] should be assignable from String[]"
        )
    }

    @Test
    fun `multi dimensional reference arrays are covariant`() {
        val objectMatrix = Portrait.forName("[[Ljava.lang.Object;")
        val stringMatrix = Portrait.forName("[[Ljava.lang.String;")

        assertTrue(
            objectMatrix.isAssignableFrom(stringMatrix),
            "Object[][] should be assignable from String[][]"
        )
    }

    @Test
    fun `primitive arrays are invariant`() {
        val objectArray = Portrait.forName("[Ljava.lang.Object;")
        val intArray = Portrait.forName("[I")

        assertFalse(
            objectArray.isAssignableFrom(intArray),
            "Object[] must not be assignable from int[]"
        )
    }

    @Test
    fun `reference arrays are subclasses of covariant parents`() {
        val objectArray = Portrait.forName("[Ljava.lang.Object;")
        val stringArray = Portrait.forName("[Ljava.lang.String;")

        assertTrue(
            stringArray.isSubclassOf(objectArray),
            "String[] should be a subclass of Object[]"
        )
        assertFalse(
            objectArray.isSubclassOf(stringArray),
            "Object[] must not be a subclass of String[]"
        )
    }

    @Test
    fun `primitive arrays are not subclasses of reference arrays`() {
        val objectArray = Portrait.forName("[Ljava.lang.Object;")
        val intArray = Portrait.forName("[I")

        assertFalse(
            intArray.isSubclassOf(objectArray),
            "int[] must not be a subclass of Object[]"
        )
    }
}
