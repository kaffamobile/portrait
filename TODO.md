# TODO

### Guarantee TeaVM support

Test `portrait-runtime-aot` with TeaVM

> See https://www.teavm.org/docs/tooling/testing.html

### Dogfooding

Remove ".wellknown" at `portrait-api`.
Replace with a run of `portrait-codegen` with a @OptInPortrait

### Post v1.0: AoT emulation in JVM

Add in JVM a flag to lock down reflection to work exactly like AoT.

### Post v1.0: Plugins

Create Maven and Gradle Plugin for portrait-codegen