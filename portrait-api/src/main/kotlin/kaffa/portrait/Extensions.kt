@file:JvmName("PortraitExtensions")

package kaffa.portrait

import kotlin.reflect.KClass

/**
 * Extension property that provides convenient access to Portrait reflection for Java classes.
 *
 * This property allows you to get a PClass wrapper directly from any Java Class instance
 * without explicitly calling Portrait.of().
 *
 * Example usage:
 * ```kotlin
 * val stringClass = String::class.java
 * val portrait = stringClass.portrait  // Instead of Portrait.of(stringClass)
 *
 * // Can be chained fluently
 * val fields = MyClass::class.java.portrait.declaredFields
 * ```
 *
 * @receiver Class<T> The Java class to wrap
 * @return PClass<T> Portrait wrapper for reflection operations
 * @throws PortraitNotFoundException if the class cannot be wrapped
 */
val <T : Any> Class<T>.portrait: PClass<T>
    get() = Portrait.of(this)

/**
 * Extension property that provides convenient access to Portrait reflection for Kotlin classes.
 *
 * This property allows you to get a PClass wrapper directly from any Kotlin KClass instance
 * without explicitly calling Portrait.of().
 *
 * Example usage:
 * ```kotlin
 * val stringClass = String::class
 * val portrait = stringClass.portrait  // Instead of Portrait.of(stringClass)
 *
 * // Can be chained fluently
 * val methods = MyClass::class.portrait.declaredMethods
 *
 * // Works with both Java and Kotlin classes
 * val javaList = List::class.portrait
 * val kotlinList = kotlin.collections.List::class.portrait
 * ```
 *
 * @receiver KClass<T> The Kotlin class to wrap
 * @return PClass<T> Portrait wrapper for reflection operations
 * @throws PortraitNotFoundException if the class cannot be wrapped
 */
val <T : Any> KClass<T>.portrait: PClass<T>
    get() = Portrait.of(this)

/**
 * Extension property that provides convenient access to Portrait reflection for object instances.
 *
 * This property allows you to get a PClass wrapper directly from any object instance
 * without explicitly calling Portrait.from().
 *
 * Example usage:
 * ```kotlin
 * val myString = "Hello"
 * val portrait = myString.portrait  // Instead of Portrait.from(myString)
 *
 * // Can be chained fluently
 * val className = myObject.portrait.simpleName
 * ```
 *
 * @receiver T The object instance to wrap
 * @return PClass<T> Portrait wrapper for reflection operations
 * @throws PortraitNotFoundException if the instance's class cannot be wrapped
 */
val <T : Any> T.portrait: PClass<T>
    get() = Portrait.from(this)