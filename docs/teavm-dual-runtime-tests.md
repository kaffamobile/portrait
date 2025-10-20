# Spec: Dual-Runtime Tests for Portrait (JVM + TeaVM)

**Owner:** Build & Infra  
**Audience:** Portrait maintainers, CI engineering  
**Status:** Draft → Ready to implement  
**Gradle DSL:** Kotlin (primary); Groovy appendix optional later  
**Applies to:** Gradle 8.x, Kotlin 1.9, TeaVM 0.9.x

---

## 1. Context

Portrait already exercises its APIs via `portrait-runtime-jvm`, `portrait-runtime-aot`, and `tests-e2e`. Each module leans on `kotlin-test` (`libs.kotlin.test` in the version catalog), which currently routes through JUnit5 because every `Test` task calls `useJUnitPlatform()`. To execute the exact same assertions under TeaVM we must bridge those Kotlin tests onto JUnit4 (`kotlin-test-junit`) and add TeaVM’s `TeaVMTestRunner`. TeaVM still exposes its runner only as a JUnit4 integration point, so a controlled migration path is required:

- Keep using `kotlin.test.*` APIs in source to avoid touching assertions/helpers.
- Swap the runtime backend depending on the target:
  - JVM path: keep JUnit5 engine (short term) while we stand up the new harness.
  - TeaVM path: introduce generated JUnit4 wrappers that apply `@RunWith(TeaVMTestRunner.class)`, then migrate the underlying backend to `kotlin-test-junit`.
- Once TeaVM execution is stable, backport the JUnit4 backend to JVM as well (so that both runtimes share a single execution model) and remove the Jupiter dependency.

---

## 2. Goals & Non-goals

**Goals**
- Run the same `kotlin-test` source set twice against `portrait-api`:
  1. With `portrait-runtime-jvm` on the classpath (`jvmTest`).
  2. With `portrait-runtime-aot` plus generated code (`teavmTest`).
- Reuse existing coverage from `portrait-runtime-jvm` and `tests-e2e` by extracting their tests into a dedicated `tests/` module.
- Generate `_TeaVM` wrapper classes into a git-ignored source set and wire them into a TeaVM-only Gradle `Test` task.
- Capture codegen + runtime wiring in a reusable Gradle plugin (lives in `buildSrc`) so the setup stays declarative.

**Non-goals**
- No changes to public Portrait APIs or provider resolution.
- No TeaVM JS/WASM CI integration beyond the described Gradle tasks (enable later).
- No direct ClassGraph/ClassLoader usage in tests — rely on Portrait entry points.

---

## 3. Module Layout

```
root/
├─ buildSrc/
│  └─ src/main/kotlin/PortraitDualRuntimePlugin.kt   # new Gradle plugin scaffolding
├─ tests/                                            # new aggregation module
│  ├─ build.gradle.kts
│  ├─ src/
│  │  ├─ sharedTest/kotlin/…                         # tests migrated from portrait-runtime-jvm + tests-e2e
│  │  ├─ sharedTest/resources/…
│  │  └─ teavmWrappers/kotlin/.gitignore             # empty placeholder; actual files generated
│  └─ .gitignore                                     # ignore `src/teavmWrappers` and `.generated/`
├─ portrait-runtime-jvm/                             # keep fixtures, drop duplicated tests
├─ portrait-runtime-aot/
├─ portrait-codegen/
└─ tests-e2e/                                        # keep fixtures that codegen consumes
```

Key ideas:
- `sharedTest` becomes the canonical test source set. Both JVM and TeaVM tasks depend on its compiled output.
- `teavmWrappers` is a synthetic Kotlin/Java source directory that stays empty in git; the plugin generates `_TeaVM` classes just-in-time during builds.
- `tests/` depends on `:portrait-api` for production code, and adds either `:portrait-runtime-jvm` or `:portrait-runtime-aot` at test runtime.

---

## 4. Execution Flow

1. **Assemble shared fixtures**
   - Move pure test data/fixtures from `portrait-runtime-jvm` & `tests-e2e` into `tests/src/sharedTest`.
   - Keep any runtime-specific fixtures (e.g. TeaVM-specific proxies) in their original modules if they rely on module-local code.

2. **JVM run (`jvmTest`)**
   - Configuration:
     - `testImplementation(project(":portrait-api"))`
     - `testRuntimeOnly(project(":portrait-runtime-jvm"))`
     - `testImplementation(libs.kotlin.test)` and `testRuntimeOnly(libs.junit.jupiter)` during the interim.
   - Task: standard `Test` with `useJUnitPlatform()` until the JUnit4 migration completes.

3. **TeaVM run (`teavmTest`)**
   - Depends on:
     - `tests:compileSharedTestKotlin`
     - Code generation task (`portrait-codegen` CLI) that targets the compiled `sharedTest` fixtures.
   - Adds to classpath:
     - `project(":portrait-api")`
     - `project(":portrait-runtime-aot")`
     - Generated portrait metadata output (same pattern as `tests-e2e` task `runCodegen`).
   - Wrapper generation:
     - Scan compiled test classes (`sharedTest`) and emit subclasses suffixed `_TeaVM`.
     - Write them under `tests/.generated/teavmWrappers` (folder lives outside `build/` but is ignored via `tests/.gitignore`).
     - Register a dedicated source set (`teavmWrappers`) so Gradle recompiles only the small generated set.
   - Execute `Test` task that uses JUnit4 and TeaVM runner:
     - `testClassesDirs = sourceSets["teavmCombined"].output.classesDirs`
     - `classpath = sourceSets["teavmCombined"].runtimeClasspath`
     - Add `libs.teavm.junit` and `kotlin-test-junit` backend.

4. **Task wiring**
   - `check` depends on both `jvmTest` and `teavmTest`.
   - CI matrix runs `./gradlew :tests:jvmTest` and `./gradlew :tests:teavmTest`.

---

## 5. Source Set Design (`tests/`)

```kotlin
sourceSets {
    val sharedTest by creating {
        kotlin.srcDir("src/sharedTest/kotlin")
        resources.srcDir("src/sharedTest/resources")
        dependencies {
            implementation(project(":portrait-api"))
            implementation(libs.kotlin.test)        // kotlin-test API surface
        }
    }

    val jvmTest by getting {
        compileClasspath += sharedTest.output + configurations["jvmTestImplementation"]
        runtimeClasspath += compileClasspath + configurations["jvmTestRuntimeOnly"]
    }

    val teavmWrappers by creating {
        kotlin.srcDir(".generated/teavmWrappers/kotlin")
        resources.srcDir("src/sharedTest/resources")
        compileClasspath += sharedTest.output + configurations["teavmTestImplementation"]
        runtimeClasspath += compileClasspath + configurations["teavmTestRuntimeOnly"]
    }
}
```

Configuration relationships:
- `configurations["jvmTestImplementation"]` extends from `sharedTestImplementation`.
- `configurations["teavmTestImplementation"]` extends from `sharedTestImplementation`.
- `configs teavmTestRuntimeOnly` extends from `sharedTestRuntimeOnly` and includes `libs.teavm.junit`, `project(":portrait-runtime-aot")`, and the codegen output directory.

Gradle tasks:
- `tasks.register<Test>("jvmTest")` -> `useJUnitPlatform()` for now.
- `tasks.register<Test>("teavmTest")`
  - depends on wrapper generation + codegen
  - `useJUnit()` (classic runner) after the backend swap.
- `tasks.named("check") { dependsOn("jvmTest", "teavmTest") }`

---

## 6. Kotlin Test Backend Strategy

| Phase | Backend | Notes |
|-------|---------|-------|
| Current | `kotlin-test` + JUnit5 (`libs.junit.jupiter`) | already wired in modules; keep while refactoring sources. |
| Bridge | Add `kotlin-test-junit` to the shared test classpath and keep both engines | Allows gradual opt-in per task; mark TeaVM task to exclude JUnit5 engine. |
| Final | Only `kotlin-test-junit4` (`useJUnit()`) | Required for TeaVM runner interoperability; JVM task switches to `useJUnit()` as soon as assertions pass. |

Action items:
1. Update `buildSrc` plugin to attach the right engine per task (`jvmTest` -> Jupiter until migration toggle flips; `teavmTest` -> vintage).
2. Track remaining Jupiter-only constructs (e.g., dynamic tests) and refactor them before enabling TeaVM task.
3. Document helper extension replacements in `tests/README.md`.

---

## 7. Code Generation & `_TeaVM` Wrappers

- Reuse the `runCodegen` pattern from `tests-e2e`:
  - Input: compiled `sharedTest` fixtures (`build/classes/kotlin/sharedTest`).
  - Output: `tests/build/generated/portrait-classes` (added via `testRuntimeOnly`).
  - Ensure we pass an explicit `--classpath` containing `portrait-api`.
- Wrapper generation pseudocode (Kotlin):

```kotlin
val generateTeaVmWrappers by tasks.registering {
    inputs.files(sharedTestOutput)
    outputs.dir(project.layout.projectDirectory.dir(".generated/teavmWrappers/kotlin"))

    doLast {
        val outputRoot = outputs.files.singleFile.apply { deleteRecursively(); mkdirs() }
        discoveredTestClasses.forEach { className ->
            writeWrapper(className, outputRoot)   // emits `<SimpleName>_TeaVM.kt`
        }
    }
}
```

- Files land in `.generated/teavmWrappers/kotlin` and remain untracked thanks to a dedicated `.gitignore`.
- Wrapper format:
  - Kotlin class extending the compiled test (`class Foo_TeaVM : Foo()`).
  - Annotated with `@RunWith(TeaVMTestRunner::class)`.
  - Import from `org.junit.runner.RunWith` and `org.teavm.junit.TeaVMTestRunner`.

---

## 8. buildSrc Plugin Sketch

Create `buildSrc/src/main/kotlin/PortraitDualRuntimePlugin.kt` that:

1. Applies `org.jetbrains.kotlin.jvm`.
2. Registers shared configurations (`sharedTest`, `jvmTest`, `teavmTest`).
3. Exposes extension properties for:
   - Adding runtime dependencies (`portrait-runtime-jvm`, `portrait-runtime-aot`).
   - Wiring `libs.kotlin.test`, `libs.teavm.junit`, `libs.junit.jupiter`.
4. Registers helper tasks:
   - `GenerateTeaVmWrappersTask`
   - `PortraitCodegenTask` (wraps the CLI invocation with incremental inputs)
   - `ConfigureDualRuntimeTests` (glue for `jvmTest`/`teavmTest` tasks).
5. Provides toggles (`useJunit5ForJvm = true/false`) to orchestrate the migration.

Consumers (only `tests/` today) simply apply `id("tech.kaffa.portrait.dual-runtime")`.

---

## 9. Migration Steps

1. **Prep**
   - Copy tests from `portrait-runtime-jvm/src/test` and `tests-e2e/src/test` into `tests/src/sharedTest`.
   - Move shared fixtures (`Operation`, `Calculator`, etc.) into `tests/src/sharedTest/kotlin`.
   - Leave module-specific fixtures behind and access them via test dependencies if required.

2. **Bootstrap module**
   - Create `tests/build.gradle.kts` that applies the new plugin.
   - Add dependencies using the version catalog (e.g., `implementation(project(":portrait-api"))`, `teavmTestRuntimeOnly(project(":portrait-runtime-aot"))`, `teavmTestRuntimeOnly(files(layout.buildDirectory.dir("generated/portrait-classes")))`, `teavmTestImplementation(libs.teavm.junit)`).

3. **Wire codegen**
   - Register a `RunPortraitCodegen` task based on `tests-e2e`.
   - Ensure `teavmTest` depends on it.

4. **Implement wrapper generation**
   - Leverage ASM or Kotlin compiler metadata to find concrete test classes (avoid loading classes with the Portrait runtime).
   - Emit Kotlin wrappers to `.generated/teavmWrappers/kotlin`.

5. **Enable TeaVM test task**
   - Configure TeaVM plugin if needed (Java 11+ requirement).
   - Verify `teavmTest` passes on CI.

6. **JUnit backend convergence**
   - Once TeaVM suite is stable, shift JVM task to JUnit4 + `kotlin-test-junit`.
   - Drop `libs.junit.jupiter` dependency from the catalog if unused elsewhere.

---

## 10. Open Questions

- Should the wrapper generator leverage existing metadata emitted by Portrait codegen to avoid custom scanners?
- Do we need a third task for running TeaVM in JS/WASM (mirroring `tests-e2e:testJs`), or do we defer until Java 11 is standard for the toolchain?
- Where should TeaVM-specific fixtures live (inside `tests/` or dedicated module)?
- How do we share generated metadata paths with IDEs (Gradle sync vs. manual IDEA configuration)?

---

## 11. Deliverables Checklist

- [ ] `tests/` module created and registered in `settings.gradle.kts`.
- [ ] `buildSrc` plugin published and applied.
- [ ] Shared tests migrated and passing on the JVM path.
- [ ] Portrait codegen wired and outputs consumed by TeaVM run.
- [ ] `_TeaVM` wrappers generated into `.gitignore`-d source set.
- [ ] TeaVM task green in CI; documentation (this spec + `tests/README.md`) updated with run commands.
- [ ] Follow-up work item filed to complete the JUnit 4 migration for JVM tasks.

