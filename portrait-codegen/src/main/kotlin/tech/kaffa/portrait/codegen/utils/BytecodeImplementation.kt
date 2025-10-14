package tech.kaffa.portrait.codegen.utils

import net.bytebuddy.dynamic.scaffold.InstrumentedType
import net.bytebuddy.implementation.Implementation
import net.bytebuddy.implementation.bytecode.ByteCodeAppender

abstract class BytecodeImplementation : Implementation, ByteCodeAppender {

    override fun prepare(instrumentedType: InstrumentedType): InstrumentedType {
        return instrumentedType
    }

    override fun appender(implementationTarget: Implementation.Target): ByteCodeAppender {
        return this
    }
}