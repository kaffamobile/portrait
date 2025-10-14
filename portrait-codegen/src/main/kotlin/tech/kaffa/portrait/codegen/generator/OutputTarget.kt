package tech.kaffa.portrait.codegen.generator

import tech.kaffa.portrait.codegen.PortraitGenerator
import java.io.Closeable

interface OutputTarget : Closeable {
    fun writeGeneratedClass(generated: PortraitGenerator.GeneratedClass)
    fun writeServiceProviderEntry(providerClassName: String)
}