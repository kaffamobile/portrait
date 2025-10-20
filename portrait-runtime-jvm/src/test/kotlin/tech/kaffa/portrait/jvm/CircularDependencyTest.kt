package tech.kaffa.portrait.jvm

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Test to verify that circular dependencies are properly handled with lazy loading.
 */
class CircularDependencyTest {

    @Test
    fun `JvmPClass creation does not cause immediate circular dependency evaluation`() {
        // This test verifies that creating a JvmPClass doesn't immediately
        // evaluate all its properties that could cause circular dependencies

        val provider = JvmPortraitProvider()

        // Try to create a PClass for a basic class
        // This should not cause StackOverflowError due to immediate evaluation
        // of superclass, interfaces, methods, fields, etc.
        val stringPClass = provider.forName<String>("java.lang.String")

        // The PClass should be created successfully
        assertNotNull(stringPClass)

        // Basic properties should be accessible without triggering circular evaluation
        assertNotNull(stringPClass.simpleName)
        assertNotNull(stringPClass.qualifiedName)

        // These properties are now lazy and won't trigger immediate evaluation
        // but we can verify they can be accessed when needed
        assertDoesNotThrow { stringPClass.superclass }
        assertDoesNotThrow { stringPClass.interfaces }
        assertDoesNotThrow { stringPClass.methods }
        assertDoesNotThrow { stringPClass.fields }
        assertDoesNotThrow { stringPClass.constructors }

        // We don't need to assert specific values since the provider setup
        // may not be complete, but we should be able to access these properties
        // without StackOverflowError
    }

    @Test
    fun `Multiple PClass creations work without circular dependency issues`() {
        val provider = JvmPortraitProvider()

        // Try to create multiple related classes that might reference each other
        val stringPClass = provider.forName<String>("java.lang.String")
        val objectPClass = provider.forName<Any>("java.lang.Object")

        assertNotNull(stringPClass)
        assertNotNull(objectPClass)

        // These operations should work without infinite recursion
        // because all PClass dependencies are now lazy-loaded
        val stringSuper = assertDoesNotThrow { stringPClass.superclass }
        val objectSuper = assertDoesNotThrow { objectPClass.superclass }

        assertNotNull(stringSuper)
        assertNull(objectSuper)

        // Even if these are null due to provider setup issues,
        // the important thing is that we don't get StackOverflowError
    }
}

private inline fun <T> assertDoesNotThrow(block: () -> T): T {
    return try {
        block()
    } catch (throwable: Throwable) {
        fail("Expected no exception, but caught ${throwable::class.simpleName}: ${throwable.message}")
    }
}
