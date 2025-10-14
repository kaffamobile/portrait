package tech.kaffa.portrait.proxy

import tech.kaffa.portrait.PMethod

/**
 * Interface for handling method invocations on dynamically created proxy instances.
 *
 * ProxyHandler provides a unified way to intercept and handle method calls on proxy objects
 * created through Portrait's reflection API. This allows for dynamic implementation of
 * interfaces and abstract classes without requiring compile-time knowledge of the target type.
 *
 * Example usage:
 * ```kotlin
 * val myInterface = MyInterface::class.java.portrait
 * val proxy = myInterface.createProxy { self, method, args ->
 *     when (method.name) {
 *         "getName" -> "Dynamic Name"
 *         "getAge" -> 25
 *         "toString" -> "Proxy[${method.declaringClass.simpleName}]"
 *         else -> throw UnsupportedOperationException("Method ${method.name} not implemented")
 *     }
 * }
 * ```
 *
 * @param T The type of the proxy being created
 */
fun interface ProxyHandler<T> {

    /**
     * Handles a method invocation on the proxy instance.
     *
     * This method is called whenever any method is invoked on the proxy object.
     * The implementation should examine the method and arguments to determine
     * the appropriate response.
     *
     * @param self The proxy instance on which the method was invoked
     * @param method The method being invoked
     * @param args The arguments passed to the method (empty array if no arguments)
     * @return The result to return from the method invocation
     * @throws Exception Any exception to propagate from the method call
     */
    fun invoke(self: T, method: PMethod, args: Array<out Any?>?): Any?
}

