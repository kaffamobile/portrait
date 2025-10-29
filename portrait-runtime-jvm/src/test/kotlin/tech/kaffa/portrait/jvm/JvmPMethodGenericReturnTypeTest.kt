package tech.kaffa.portrait.jvm

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import tech.kaffa.portrait.PGenericArrayType
import tech.kaffa.portrait.PMethod
import tech.kaffa.portrait.PParameterizedType
import tech.kaffa.portrait.PTypeVariable
import tech.kaffa.portrait.PWildcardType
import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.typeName

class JvmPMethodGenericReturnTypeTest {

    @BeforeTest
    fun clearPortraitCache() {
        Portrait.clearCache()
    }

    @Test
    fun `parameterized return type preserves arguments`() {
        val method = methodNamed("strings")

        val genericType = method.genericReturnType
        val parameterized = assertIs<PParameterizedType>(genericType)

        assertEquals("java.util.List<java.lang.String>", parameterized.typeName())
        assertEquals("java.util.List", parameterized.rawType.qualifiedName)
        assertEquals(1, parameterized.arguments.size)
    }

    @Test
    fun `type variable return type surfaces symbol`() {
        val method = methodNamed("identity")

        val genericType = method.genericReturnType
        val typeVariable = assertIs<PTypeVariable>(genericType)

        assertEquals("T", typeVariable.name)
        assertEquals("T", typeVariable.typeName())
    }

    @Test
    fun `wildcard bounds are captured`() {
        val extendsMethod = methodNamed("wildcardExtends")
        val superMethod = methodNamed("wildcardSuper")

        val extendsType = assertIs<PParameterizedType>(extendsMethod.genericReturnType)
        val extendsArgument = assertIs<PWildcardType>(extendsType.arguments.single())
        assertEquals("? extends java.lang.Number", extendsArgument.typeName())

        val superType = assertIs<PParameterizedType>(superMethod.genericReturnType)
        val superArgument = assertIs<PWildcardType>(superType.arguments.single())
        assertEquals("? super java.lang.Number", superArgument.typeName())
    }

    @Test
    fun `generic array return type captures component`() {
        val method = methodNamed("genericArray")

        val genericType = assertIs<PGenericArrayType>(method.genericReturnType)
        assertEquals("T[]", genericType.typeName())
        val component = assertIs<PTypeVariable>(genericType.componentType)
        assertEquals("T", component.name)
    }

    private fun methodNamed(name: String): PMethod {
        val pClass = Portrait.of(GenericFixtures::class.java)
        return pClass.methods.first { it.name == name }
    }
}
