package tech.kaffa.portrait.jvm

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tech.kaffa.portrait.Portrait

abstract class FlagAbstract

sealed class FlagSealed {
    class Implementation : FlagSealed()
}

data class FlagData(val value: String)

class FlagWithCompanion {
    companion object
}

enum class FlagEnum {
    FIRST
}

interface FlagInterface

class FlagRegular

class JvmPClassFlagsTest {

    @BeforeTest
    fun resetPortraitCache() {
        Portrait.clearCache()
    }

    @Test
    fun `abstract classes are reported as abstract`() {
        val pClass = Portrait.of(FlagAbstract::class)

        assertTrue(pClass.isAbstract)
        assertFalse(pClass.isSealed)
        assertFalse(pClass.isData)
        assertFalse(pClass.isCompanion)
        assertFalse(pClass.isEnum)
        assertFalse(pClass.isInterface)
    }

    @Test
    fun `sealed classes are reported as sealed`() {
        val pClass = Portrait.of(FlagSealed::class)

        assertTrue(pClass.isSealed)
        assertTrue(pClass.isAbstract, "sealed classes should also be considered abstract")
    }

    @Test
    fun `data classes are reported as data`() {
        val pClass = Portrait.of(FlagData::class)

        assertTrue(pClass.isData)
        assertFalse(pClass.isAbstract)
        assertFalse(pClass.isSealed)
    }

    @Test
    fun `companion objects are reported as companions`() {
        val pClass = Portrait.of(FlagWithCompanion.Companion::class)

        assertTrue(pClass.isCompanion)
        assertFalse(pClass.isEnum)
        assertFalse(pClass.isInterface)
    }

    @Test
    fun `enums are reported as enums`() {
        val pClass = Portrait.of(FlagEnum::class)

        assertTrue(pClass.isEnum)
        assertFalse(pClass.isAbstract)
        assertFalse(pClass.isInterface)
    }

    @Test
    fun `interfaces are reported as interfaces`() {
        val pClass = Portrait.of(FlagInterface::class)

        assertTrue(pClass.isInterface)
        assertTrue(pClass.isAbstract, "interfaces should be abstract")
    }

    @Test
    fun `regular classes have no special flags`() {
        val pClass = Portrait.of(FlagRegular::class)

        assertFalse(pClass.isAbstract)
        assertFalse(pClass.isSealed)
        assertFalse(pClass.isData)
        assertFalse(pClass.isCompanion)
        assertFalse(pClass.isEnum)
        assertFalse(pClass.isInterface)
    }
}
