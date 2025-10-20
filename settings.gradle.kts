rootProject.name = "portrait"

include(
    ":portrait-annotations",
    ":portrait-api",
    ":portrait-runtime-jvm",
    ":portrait-runtime-aot",
    ":portrait-codegen",
    ":tests",
    ":tests:jvm",
    ":tests:aot"
)
