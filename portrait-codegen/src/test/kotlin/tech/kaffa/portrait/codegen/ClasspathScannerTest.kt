package tech.kaffa.portrait.codegen

import kotlin.test.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import tech.kaffa.portrait.codegen.utils.ClasslibConfiguration
import tech.kaffa.portrait.codegen.utils.ClasslibLocatorFactory

private fun newScanner(classpath: String): ClasspathScanner {
    val locator = ClasslibLocatorFactory.create(ClasslibConfiguration.forCurrentRuntime())
    return ClasspathScanner(classpath, locator)
}

class ClasspathScannerTest {

    @Test
    fun `ClasspathScanner can scan empty classpath`() {
        // Test with empty classpath - should not crash
        newScanner("").scan().use { result ->
            assertNotNull(result)
            assertNotNull(result.reflectives)
            assertNotNull(result.proxyTargets)
        }
    }

    @Test
    fun `ClasspathScanner can scan current classpath`() {
        // Use current classpath which should include test classes
        val currentClasspath = System.getProperty("java.class.path")

        newScanner(currentClasspath).scan().use { result ->
            assertNotNull(result)
            assertNotNull(result.reflectives)
            assertNotNull(result.proxyTargets)
        }
    }

    @Test
    fun `ClasspathScanner Result has correct structure`() {
        val currentClasspath = System.getProperty("java.class.path")
        newScanner(currentClasspath).scan().use { result ->
            // Verify Result class structure
            assertIs<Iterable<*>>(result.reflectives)
            assertIs<Iterable<*>>(result.proxyTargets)
        }
    }

    @Test
    fun `ClasspathScanner handles multiple classpath entries`() {
        // Create a multi-entry classpath
        val separator = File.pathSeparator
        val currentClasspath = System.getProperty("java.class.path")
        val multipleClasspath = "$currentClasspath${separator}$currentClasspath"

        newScanner(multipleClasspath).scan().use { result ->
            assertNotNull(result)
            // Should handle duplicate entries gracefully
        }
    }

    @Test
    fun `ClasspathScanner handles invalid classpath entries gracefully`() {
        val separator = File.pathSeparator
        val invalidPath = "/nonexistent/path/to/classes.jar"
        val currentClasspath = System.getProperty("java.class.path")
        val mixedClasspath = "$currentClasspath${separator}$invalidPath"

        // Should not crash even with invalid entries
        newScanner(mixedClasspath).scan().use { result ->
            assertNotNull(result)
        }
    }

    @Test
    fun `ClasspathScanner can find test fixture classes`() {
        newScanner("").scan().use { result ->
            val reflectiveClasses = result.reflectives.toList()
            val proxyTargetClasses = result.proxyTargets.toList()

            // Check if we can find our test fixture classes

            // Test fixtures should be found if they have the annotations
            // This is dependent on the test fixture classes being properly annotated
            assertTrue(reflectiveClasses.isNotEmpty()) // Either found or empty is ok for unit tests
            assertTrue(proxyTargetClasses.isNotEmpty()) // Either found or empty is ok for unit tests
        }
    }

    @Test
    fun `ClasspathScanner can handle jar files in classpath`() {
        // This test verifies that the scanner can handle jar files
        // In a real environment, classpath often contains jar files
        val currentClasspath = System.getProperty("java.class.path")

        // Filter for jar files in classpath
        val separator = File.pathSeparator
        val classpathEntries = currentClasspath.split(separator)
        val jarEntries = classpathEntries.filter { it.endsWith(".jar") }

        if (jarEntries.isNotEmpty()) {
            val jarClasspath = jarEntries.joinToString(separator)
            newScanner(jarClasspath).scan().use { result ->
                assertNotNull(result)
            }
        }
    }

    @Test
    fun `ClasspathScanner handles directory classpath entries`() {
        // Test with directory entries (common in development environments)
        val currentClasspath = System.getProperty("java.class.path")
        val separator = File.pathSeparator
        val classpathEntries = currentClasspath.split(separator)

        // Filter for directory entries (not jar files)
        val directoryEntries = classpathEntries.filterNot { it.endsWith(".jar") }

        if (directoryEntries.isNotEmpty()) {
            val directoryClasspath = directoryEntries.joinToString(separator)
            newScanner(directoryClasspath).scan().use { result ->
                assertNotNull(result)
            }
        }
    }

    @Test
    fun `ClasspathScanner Result iterables are reusable`() {
        val currentClasspath = System.getProperty("java.class.path")
        newScanner(currentClasspath).scan().use { result ->
            // Verify that iterables can be iterated multiple times
            val firstIteration = result.reflectives.toList()
            val secondIteration = result.reflectives.toList()

            assertEquals(firstIteration.size, secondIteration.size)

            val firstProxyIteration = result.proxyTargets.toList()
            val secondProxyIteration = result.proxyTargets.toList()

            assertEquals(firstProxyIteration.size, secondProxyIteration.size)
        }
    }

    @Test
    fun `public api include options add related types`() {
        val currentClasspath = System.getProperty("java.class.path")

        newScanner(currentClasspath).scan().use { result ->
            val reflectiveNames = result.reflectives

            assertTrue("tech.kaffa.portrait.codegen.PublicApiSubtypeRoot" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSubtypeBase" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSubtypeContract" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSubtypeImpl" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSubtypeContractImpl" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSupertypeRoot" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSupertypeImpl" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiSupertypeBase" in reflectiveNames)
            assertTrue("tech.kaffa.portrait.codegen.PublicApiStringRoot" in reflectiveNames)
            assertTrue("java.lang.String" in reflectiveNames, "Missing java.lang.String. Found: $reflectiveNames")
        }
    }
}

