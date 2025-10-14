package kaffa.portrait.e2e.fixtures

import kaffa.portrait.Reflective
import kaffa.portrait.ProxyTarget
import kaffa.portrait.Includes

/**
 * Simple class with basic reflection support for E2E testing.
 */
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

/**
 * Abstract class with sealed hierarchy for testing inheritance.
 */
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

/**
 * Interface with proxy support for E2E testing.
 */
@ProxyTarget
interface Calculator {
    fun add(a: Int, b: Int): Int
    fun multiply(a: Int, b: Int): Int
}

/**
 * Service class with multiple methods for testing method reflection.
 */
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

    fun processString(input: String): String {
        return input.uppercase()
    }

    fun processVarargs(vararg numbers: Int): Int {
        return numbers.sum()
    }
}

/**
 * Object for testing singleton/object reflection.
 */
@Reflective
object SingletonService {
    private var counter: Int = 0

    fun incrementCounter(): Int {
        counter++
        return counter
    }

    fun getCounter(): Int = counter
}

/**
 * Enum class for testing enum reflection.
 */
@Reflective
enum class Status {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED;

    fun isTerminal(): Boolean = this == COMPLETED || this == FAILED
}

/**
 * Class with various field types for testing field reflection.
 */
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

/**
 * Class with multiple constructors for testing constructor reflection.
 */
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

/**
 * Generic class for testing generic type handling.
 */
@Reflective
data class Container<T>(val content: T) {
    fun get(): T = content

    fun transform(transformer: (T) -> T): Container<T> {
        return Container(transformer(content))
    }
}

/**
 * Class with nullable types for testing null handling.
 */
@Reflective
data class NullableTestClass(
    val required: String,
    val optional: String?
) {
    fun getOptionalOrDefault(default: String): String {
        return optional ?: default
    }
}
