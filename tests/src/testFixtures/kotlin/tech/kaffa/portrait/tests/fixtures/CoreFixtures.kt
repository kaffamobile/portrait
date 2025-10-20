@file:Suppress("unused", "UNUSED_PARAMETER")

package tech.kaffa.portrait.tests.fixtures

import tech.kaffa.portrait.Includes
import tech.kaffa.portrait.ProxyTarget
import tech.kaffa.portrait.Reflective

@Reflective
@ProxyTarget
data class TestDataClass(
    @JvmField val id: Int,
    @JvmField val name: String,
    val active: Boolean = true
) {
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

    @field:TestAnnotation("field-level")
    @JvmField
    var annotatedField: String = "test"

    @TestAnnotation("method-level", 200)
    fun annotatedMethod(): String = "annotated"
}

@TestAnnotation
@Reflective
class SimpleAnnotatedClass

@Reflective
class ParameterTestClass {

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

    fun objectParams(
        stringVal: String,
        listVal: List<String>,
        mapVal: Map<String, Int>
    ): String = "objects"

    fun arrayParams(
        intArray: IntArray,
        stringArray: Array<String>
    ): String = "arrays"

    fun varargParams(vararg values: String): String = values.joinToString()

    fun <T> genericMethod(value: T): T = value
}

@Reflective
class ExceptionTestClass {
    fun throwsException(): String {
        throw RuntimeException("Test exception")
    }

    fun throwsChecked(): String {
        throw Exception("Checked exception")
    }
}

object TestUtils {
    fun createTestDataClass(id: Int = 1, name: String = "test"): TestDataClass {
        return TestDataClass(id, name)
    }

    fun createTestClass(value: String = "test"): TestClass {
        return TestClass(value)
    }
}

@Reflective(including = [Includes.PUBLIC_API])
data class SimpleReflectiveClass(
    val name: String,
    val value: Int
) {
    fun greet(): String = "Hello, $name!"
    fun calculate(multiplier: Int): Int = value * multiplier

    companion object {
        const val DEFAULT_VALUE = 42

        fun create(name: String): SimpleReflectiveClass {
            return SimpleReflectiveClass(name, DEFAULT_VALUE)
        }
    }
}

@Reflective(including = [Includes.ALL_SUBTYPES])
sealed class Operation {
    abstract fun execute(): Int
}

@Reflective
data class Addition(val a: Int, val b: Int) : Operation() {
    override fun execute(): Int = a + b
}

@Reflective
data class Multiplication(val a: Int, val b: Int) : Operation() {
    override fun execute(): Int = a * b
}

@ProxyTarget
interface Calculator {
    fun add(a: Int, b: Int): Int
    fun multiply(a: Int, b: Int): Int
}

@Reflective
class ServiceClass {
    private var state: Int = 0

    fun increment(): Int {
        state++
        return state
    }

    fun reset() {
        state = 0
    }

    fun getState(): Int = state

    fun processString(input: String): String = input.uppercase()

    fun processVarargs(vararg numbers: Int): Int = numbers.sum()
}

@Reflective
object SingletonService {
    private var counter: Int = 0

    fun incrementCounter(): Int {
        counter++
        return counter
    }

    fun getCounter(): Int = counter
}

@Reflective
enum class Status {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED;

    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED
}

@Reflective
class FieldTestClass {
    @JvmField
    var publicField: String = "public"
    private var privateField: Int = 42
    val readOnlyField: Double = 3.14

    fun getPrivateField(): Int = privateField

    fun setPrivateField(value: Int) {
        privateField = value
    }
}

@Reflective(including = [Includes.PUBLIC_API])
class MultiConstructorClass {
    val name: String
    val value: Int
    val optional: String?

    constructor(name: String) {
        this.name = name
        this.value = 0
        this.optional = null
    }

    constructor(name: String, value: Int) {
        this.name = name
        this.value = value
        this.optional = null
    }

    constructor(name: String, value: Int, optional: String) {
        this.name = name
        this.value = value
        this.optional = optional
    }
}

@Reflective
data class Container<T>(val content: T) {
    fun get(): T = content

    fun transform(transformer: (T) -> T): Container<T> {
        return Container(transformer(content))
    }
}

@Reflective
data class NullableTestClass(
    val required: String,
    val optional: String?
) {
    fun getOptionalOrDefault(default: String): String {
        return optional ?: default
    }
}
