package tech.kaffa.portrait.tests

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.tests.fixtures.KotlinAbstractFixture
import tech.kaffa.portrait.tests.fixtures.KotlinCompanionFixture
import tech.kaffa.portrait.tests.fixtures.KotlinDataFixture
import tech.kaffa.portrait.tests.fixtures.KotlinEnumFixture
import tech.kaffa.portrait.tests.fixtures.KotlinInterfaceFixture
import tech.kaffa.portrait.tests.fixtures.KotlinSealedFixture
import tech.kaffa.portrait.tests.fixtures.TestInterface
import tech.kaffa.portrait.tests.fixtures.TestSingleton

class PClassFlagsTest {

    @Before
    fun clearPortraitCache() {
        Portrait.clearCache()
    }

    @Test
    fun abstractClassesExposeAbstractFlag() {
        val pClass = Portrait.of(KotlinAbstractFixture::class.java)

        assertTrue(pClass.isAbstract)
        assertFalse(pClass.isSealed)
    }

    @Test
    fun sealedClassesExposeSealedFlag() {
        val pClass = Portrait.of(KotlinSealedFixture::class.java)

        assertTrue(pClass.isSealed)
        assertTrue(pClass.isAbstract)
    }

    @Test
    fun dataClassesExposeDataFlag() {
        val pClass = Portrait.of(KotlinDataFixture::class.java)

        assertTrue(pClass.isData)
        assertFalse(pClass.isAbstract)
    }

    @Test
    fun companionObjectsExposeCompanionFlag() {
        val pClass = Portrait.of(KotlinCompanionFixture.Companion::class.java)

        assertTrue(pClass.isCompanion)
        assertFalse(pClass.isEnum)
    }

    @Test
    fun enumsExposeEnumFlag() {
        val pClass = Portrait.of(KotlinEnumFixture::class.java)

        assertTrue(pClass.isEnum)
        assertFalse(pClass.isInterface)
    }

    @Test
    fun interfacesExposeInterfaceFlag() {
        val kotlinInterface = Portrait.of(KotlinInterfaceFixture::class.java)
        val javaInterface = Portrait.of(TestInterface::class.java)

        assertTrue(kotlinInterface.isInterface)
        assertTrue(javaInterface.isInterface)
    }

    @Test
    fun regularClassesDoNotExposeFlags() {
        val regular = Portrait.of(TestSingleton::class.java)

        assertFalse(regular.isAbstract)
        assertFalse(regular.isSealed)
        assertFalse(regular.isData)
        assertFalse(regular.isCompanion)
        assertFalse(regular.isEnum)
        assertFalse(regular.isInterface)
    }
}
