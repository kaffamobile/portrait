@file:Suppress("unused")

package tech.kaffa.portrait.aot

import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.ProxyTarget
import tech.kaffa.portrait.Reflective

/**
 * Common test fixtures and sample classes used across Portrait tests.
 */

@Reflective
@ProxyTarget
data class TestDataClass(
    @JvmField
    val id: Int,
    @JvmField
    val name: String,
    @JvmField
    val active: Boolean = true
) {
    fun getId(): Int = id
    fun getName(): String = name
    fun isActive(): Boolean = active

    fun updateName(newName: String): TestDataClass = copy(name = newName)

    companion object {
        const val DEFAULT_NAME = "test"

        fun create(id: Int): TestDataClass = TestDataClass(id, DEFAULT_NAME)
    }
}

@Reflective
interface TestInterface {
    fun doSomething(): String
    fun processValue(value: Int): Int
    fun getName(): String
}

@Reflective
class TestClass : TestInterface {
    private var internalValue: String = "initial"

    constructor() : this("default")

    constructor(value: String) {
        this.internalValue = value
    }

    override fun doSomething(): String = "did something with $internalValue"

    override fun processValue(value: Int): Int = value * 2

    override fun getName(): String = internalValue

    fun setInternalValue(value: String) {
        this.internalValue = value
    }

    fun getInternalValue(): String = internalValue

    companion object {
        @JvmStatic
        fun staticMethod(): String = "static result"
    }
}

@Reflective
abstract class TestAbstractClass {
    abstract fun abstractMethod(): String

    open fun concreteMethod(): String = "concrete"

    fun finalMethod(): String = "final"
}

@Reflective
class TestConcreteClass : TestAbstractClass() {
    override fun abstractMethod(): String = "implemented"

    override fun concreteMethod(): String = "overridden"
}

@Reflective
object TestSingleton {
    fun getSingletonValue(): String = "singleton"
}

@Reflective
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class TestAnnotation(
    val value: String = "default",
    val number: Int = 42,
    val flag: Boolean = false
)

@TestAnnotation("class-level", 100, true)
@Reflective
class AnnotatedTestClass {

    @TestAnnotation("field-level")
    var annotatedField: String = "test"

    @TestAnnotation("method-level", 200)
    fun annotatedMethod(): String = "annotated"
}

/**
 * Test cases for various parameter types and edge cases
 */
@Reflective
class ParameterTestClass {

    @Suppress("UNUSED_PARAMETER")
    fun primitiveParams(
        byteVal: Byte,
        shortVal: Short,
        intVal: Int,
        longVal: Long,
        floatVal: Float,
        doubleVal: Double,
        boolVal: Boolean,
        charVal: Char
    ): String = "primitives"

    @Suppress("UNUSED_PARAMETER")
    fun objectParams(
        stringVal: String,
        listVal: List<String>,
        mapVal: Map<String, Int>
    ): String = "objects"

    @Suppress("UNUSED_PARAMETER")
    fun arrayParams(
        intArray: IntArray,
        stringArray: Array<String>
    ): String = "arrays"

    fun varargParams(vararg values: String): String = values.joinToString()

    fun <T> genericMethod(value: T): T = value
}

/**
 * Exception test cases
 */
@Reflective
class ExceptionTestClass {

    fun throwsException(): String {
        throw RuntimeException("Test exception")
    }

    fun throwsChecked(): String {
        throw Exception("Checked exception")
    }
}

/**
 * Utility object for test assertions and common operations
 */
object TestUtils {

    fun createTestDataClass(id: Int = 1, name: String = "test"): TestDataClass {
        return TestDataClass(id, name)
    }

    fun createTestClass(value: String = "test"): TestClass {
        return TestClass(value)
    }

    inline fun <reified T> assertType(value: Any): T {
        require(value is T) { "Expected ${T::class.simpleName}, got ${value::class.simpleName}" }
        return value
    }

    fun assertPortraitNotNull(portrait: PClass<*>?) {
        requireNotNull(portrait) { "Portrait should not be null" }
    }
}