import tech.kaffa.portrait.Portrait
import tech.kaffa.portrait.aot.StaticPClass

fun main() {
    println((Portrait.of(String::class) as StaticPClass).classEntry)
}