package tech.kaffa.portrait.codegen.provider

import net.bytebuddy.ByteBuddy
import net.bytebuddy.asm.AsmVisitorWrapper
import net.bytebuddy.description.modifier.Ownership
import net.bytebuddy.description.modifier.Visibility
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.DynamicType
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.jar.asm.ClassWriter
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import tech.kaffa.portrait.PClass
import tech.kaffa.portrait.aot.StaticPortrait
import tech.kaffa.portrait.codegen.PortraitGenerator
import tech.kaffa.portrait.codegen.portrait.PortraitClassFactory
import tech.kaffa.portrait.provider.PortraitProvider

/**
 * Factory for generating a PortraitProvider that provides all generated Portrait classes.
 *
 * This factory creates a provider class that implements PortraitProvider and contains
 * static mappings to all the generated Portrait classes from the current codegen execution.
 */
class GeneratedPortraitProviderFactory(
    private val byteBuddy: ByteBuddy,
    private val typePool: TypePool
) {
    data class Result(
        override val dynamicType: DynamicType,
        val providerClassName: String
    ) : PortraitGenerator.GeneratedClass

    /**
     * Creates a GeneratedPortraitProvider class that provides all the given portrait classes.
     *
     * @param generatedPortraits List of generated portrait results with their DynamicTypes
     * @param packageName Package name for the generated provider (default: tech.kaffa.portrait.generated)
     * @return Result containing the generated provider class
     */
    fun make(
        generatedPortraits: Set<PortraitClassFactory.Result>,
        packageName: String = "tech.kaffa.portrait.generated"
    ): Result {
        val providerClassName = "$packageName.GeneratedPortraitProvider"

        // Create mapping from original class name to portrait class name
        val portraitMap = generatedPortraits.associate { result ->
            result.superType.name to result.dynamicType.typeDescription.name
        }

        val shards = portraitMap.entries.groupBy { entry ->
            entry.key.firstOrNull()?.code ?: -1
        }

        val shardMethodNames = mutableMapOf<Int, String>()
        val shardKeys = shards.keys.filter { it >= 0 }.sorted()

        var builder = byteBuddy
            .subclass(PortraitProvider::class.java)
            .name(providerClassName)
            .defineMethod(
                "priority",
                TypeDescription.ForLoadedType.of(Int::class.javaPrimitiveType!!),
                Visibility.PUBLIC
            )
            .intercept(FixedValue.value(150)) // Priority 150 (between JVM=100 and WellKnown=200)

        shardKeys.forEach { codePoint ->
            val shardEntries = shards.getValue(codePoint)
            val shardMap = shardEntries.associate { it.key to it.value }
            val suffix = codePoint.takeIf { it in Char.MIN_VALUE.code..Char.MAX_VALUE.code }
                ?.toChar()
                ?.takeIf { it.isLetterOrDigit() }
                ?.toString()
                ?: "u${codePoint.toString(16).padStart(4, '0')}"
            val methodName = "portraitForName_$suffix"
            shardMethodNames[codePoint] = methodName

            builder = builder
                .defineMethod(methodName, StaticPortrait::class.java, Visibility.PRIVATE, Ownership.STATIC)
                .withParameters(String::class.java)
                .intercept(PortraitForFirstCharMethodImpl(shardMap))
        }

        val firstChars = shardKeys.toIntArray()

        val dynamicType = builder
            .defineMethod("forName", PClass::class.java, Visibility.PUBLIC)
            .withParameters(String::class.java)
            .intercept(PortraitForNameMethodImpl(firstChars, shardMethodNames))
            .visit(
                AsmVisitorWrapper.ForDeclaredMethods()
                    .writerFlags(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
            )
            .make(typePool)

        return Result(dynamicType, providerClassName)
    }

}
