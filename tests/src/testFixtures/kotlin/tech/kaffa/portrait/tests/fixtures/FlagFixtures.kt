package tech.kaffa.portrait.tests.fixtures

import tech.kaffa.portrait.Reflective

@Reflective
abstract class KotlinAbstractFixture

@Reflective
sealed class KotlinSealedFixture {
    @Reflective
    class Case : KotlinSealedFixture()
}

@Reflective
data class KotlinDataFixture(val value: String)

@Reflective
class KotlinCompanionFixture {
    companion object
}

@Reflective
enum class KotlinEnumFixture {
    FIRST
}

@Reflective
interface KotlinInterfaceFixture
