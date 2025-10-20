rootProject.name = "portrait"

include(
    ":portrait-annotations",
    ":portrait-api",
    ":portrait-runtime-jvm",
    ":portrait-runtime-aot",
    ":portrait-codegen",
    ":portrait-codegen:teavm-classlib",
    ":tests",
    ":tests:runtime-jvm",
    ":tests:runtime-aot"
)
