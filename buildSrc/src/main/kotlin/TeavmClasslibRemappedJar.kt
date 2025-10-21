package tech.kaffa.portrait.codegen.build

import java.io.File
import java.nio.file.Files
import java.util.Properties
import java.util.jar.JarFile
import javax.inject.Inject
import kotlin.io.path.isRegularFile
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

@DisableCachingByDefault(because = "Task performs custom bytecode rewriting before archiving")
abstract class TeavmClasslibRemappedJar @Inject constructor() : Jar() {
    @get:InputFiles
    @get:Classpath
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    abstract val remappedClassesDir: DirectoryProperty

    init {
        archiveFileName.convention("teavm-classlib-remapped.jar")
        destinationDirectory.convention(project.layout.buildDirectory.dir("generated/teavm"))
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        remappedClassesDir.convention(
            project.layout.buildDirectory.dir("generated/teavm/${name}/classes")
        )
        from(remappedClassesDir)
        doFirst { generateRemappedClasses() }
    }

    private fun generateRemappedClasses() {
        val inputFiles = sources.files
        val remappedDir = remappedClassesDir.get().asFile
        if (remappedDir.exists()) {
            remappedDir.deleteRecursively()
        }
        if (inputFiles.isEmpty()) {
            project.logger.warn("TeaVM remapping skipped: no files resolved for task $name.")
            return
        }

        remappedDir.mkdirs()

        val rules = MappingRules()
        inputFiles.forEach { rules.consumeTeaVmProperties(it) }
        rules.freeze()

        if (!rules.hasMappings()) {
            project.logger.warn(
                "TeaVM remapping found no META-INF/teavm.properties files in ${inputFiles.size} inputs."
            )
        }

        val remapper = TeaVmRemapper(rules)
        val writtenEntries = mutableSetOf<String>()

        inputFiles.forEach { file ->
            when {
                file.isDirectory -> writeDirectory(remapper, file, remappedDir, writtenEntries)
                file.extension.equals("jar", ignoreCase = true) -> writeJar(remapper, file, remappedDir, writtenEntries)
                else -> project.logger.debug("Skipping TeaVM remap input $file (unsupported type)")
            }
        }
    }

    private fun writeDirectory(
        remapper: TeaVmRemapper,
        directory: File,
        targetDir: File,
        writtenEntries: MutableSet<String>
    ) {
        Files.walk(directory.toPath()).use { paths ->
            paths.filter { it.isRegularFile() && it.fileName.toString().endsWith(".class") }
                .forEach { path ->
                    val bytes = Files.readAllBytes(path)
                    writeClassBytes(remapper, bytes, targetDir, writtenEntries)
                }
        }
    }

    private fun writeJar(
        remapper: TeaVmRemapper,
        jarFile: File,
        targetDir: File,
        writtenEntries: MutableSet<String>
    ) {
        JarFile(jarFile).use { jar ->
            val entries = jar.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.endsWith(".class") || entry.name == "module-info.class") {
                    continue
                }
                jar.getInputStream(entry).use { input ->
                    val bytes = input.readBytes()
                    writeClassBytes(remapper, bytes, targetDir, writtenEntries)
                }
            }
        }
    }

    private fun writeClassBytes(
        remapper: TeaVmRemapper,
        classBytes: ByteArray,
        targetDir: File,
        writtenEntries: MutableSet<String>
    ) {
        val reader = ClassReader(classBytes)
        if (reader.className == "module-info") {
            return
        }
        val classNode = ClassNode()
        reader.accept(classNode, 0)
        applySuperClassOverride(classNode)

        val targetInternalName = remapper.map(classNode.name) ?: classNode.name
        val writer = ClassWriter(0)
        val classRemapper = ClassRemapper(writer, remapper)
        classNode.accept(classRemapper)
        val remappedBytes = writer.toByteArray()

        val relativePath = "$targetInternalName.class"
        if (!writtenEntries.add(relativePath)) {
            project.logger.warn("Skipping duplicate remapped class entry $relativePath")
            return
        }

        val outputFile = File(targetDir, relativePath.replace('/', File.separatorChar))
        outputFile.parentFile.mkdirs()
        Files.write(outputFile.toPath(), remappedBytes)
    }
}

private class TeaVmRemapper(
    private val rules: MappingRules
) : Remapper() {
    override fun map(internalName: String?): String? {
        if (internalName == null) {
            return null
        }
        return rules.mapInternalName(internalName)
    }

    override fun mapPackageName(name: String?): String? {
        if (name == null) {
            return null
        }
        val mapped = rules.mapPackage(name.replace('/', '.'))
        return mapped.replace('.', '/')
    }
}

private const val TEAVM_SUPERCLASS_ANNOTATION = "Lorg/teavm/interop/Superclass;"

private class MappingRules {
    private val classMappings = mutableMapOf<String, String>()
    private val packageRules = mutableListOf<PackageRule>()
    private val prefixRules = mutableListOf<PrefixRule>()

    fun consumeTeaVmProperties(source: File) {
        if (source.isDirectory) {
            val propertiesFile = File(source, "META-INF/teavm.properties")
            if (propertiesFile.isFile) {
                propertiesFile.inputStream().use { input ->
                    val properties = Properties()
                    properties.load(input)
                    load(properties)
                }
            }
        } else if (source.extension.equals("jar", ignoreCase = true)) {
            JarFile(source).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name == "META-INF/teavm.properties") {
                        jar.getInputStream(entry).use { input ->
                            val properties = Properties()
                            properties.load(input)
                            load(properties)
                        }
                    }
                }
            }
        }
    }

    fun hasMappings(): Boolean = classMappings.isNotEmpty() || packageRules.isNotEmpty() || prefixRules.isNotEmpty()

    fun freeze() {
        packageRules.sortByDescending { it.specificity }
        prefixRules.sortByDescending { it.specificity }
    }

    fun mapInternalName(originalInternal: String): String {
        val dotted = originalInternal.replace('/', '.')
        classMappings[dotted]?.let { mapped ->
            return mapped.replace('.', '/')
        }

        val lastDot = dotted.lastIndexOf('.')
        val packageName = if (lastDot >= 0) dotted.substring(0, lastDot) else ""
        val simpleName = if (lastDot >= 0) dotted.substring(lastDot + 1) else dotted

        val strippedSimpleName = stripSimpleName(packageName, simpleName)
        val mappedPackage = mapPackage(packageName)

        val dottedResult = if (mappedPackage.isEmpty()) {
            strippedSimpleName
        } else {
            "$mappedPackage.$strippedSimpleName"
        }
        return dottedResult.replace('.', '/')
    }

    fun mapPackage(originalPackage: String): String {
        val rule = packageRules.firstOrNull { it.matches(originalPackage) } ?: return originalPackage
        return rule.map(originalPackage)
    }

    private fun stripSimpleName(packageName: String, simpleName: String): String {
        val rule = prefixRules.firstOrNull { it.matches(packageName) } ?: return simpleName
        return rule.strip(simpleName)
    }

    private fun load(properties: Properties) {
        for (entry in properties.entries) {
            val key = entry.key as String
            val value = entry.value as String
            val segments = key.split("|", limit = 2)
            val directive = segments[0]
            val subject = segments.getOrElse(1) { "" }
            when (directive) {
                MAP_PACKAGE_HIERARCHY -> packageRules += PackageRule(subject, value, hierarchical = true)
                MAP_PACKAGE -> packageRules += PackageRule(subject, value, hierarchical = false)
                MAP_CLASS -> classMappings[subject] = value
                STRIP_PREFIX_PACKAGE_HIERARCHY -> prefixRules += PrefixRule(subject, value, hierarchical = true)
                STRIP_PREFIX_PACKAGE -> prefixRules += PrefixRule(subject, value, hierarchical = false)
            }
        }
    }

    companion object {
        private const val MAP_CLASS = "mapClass"
        private const val MAP_PACKAGE = "mapPackage"
        private const val MAP_PACKAGE_HIERARCHY = "mapPackageHierarchy"
        private const val STRIP_PREFIX_PACKAGE = "stripPrefixFromPackageClasses"
        private const val STRIP_PREFIX_PACKAGE_HIERARCHY = "stripPrefixFromPackageHierarchyClasses"
    }
}

private fun applySuperClassOverride(classNode: ClassNode) {
    val annotation = (classNode.visibleAnnotations.orEmpty() + classNode.invisibleAnnotations.orEmpty())
        .firstOrNull { it.desc == TEAVM_SUPERCLASS_ANNOTATION }
        ?: return

    val override = annotation.extractSuperclassValue() ?: return
    val internal = override.ifEmpty { null }
    classNode.superName = internal?.takeUnless { it == classNode.name }
}

private fun AnnotationNode.extractSuperclassValue(): String? {
    val values = this.values ?: return null
    val keyIndex = values.indexOf("value")
    if (keyIndex < 0 || keyIndex + 1 >= values.size) {
        return null
    }
    val rawValue = values[keyIndex + 1] as? String ?: return null
    return if (rawValue.isEmpty()) {
        ""
    } else {
        rawValue.replace('.', '/')
    }
}

private data class PackageRule(
    val source: String,
    val target: String,
    val hierarchical: Boolean
) {
    private val sourcePrefix = if (source.isEmpty()) "" else "$source."
    private val sourceSegments = if (source.isBlank()) 0 else source.split('.').size
    val specificity: Int = (if (hierarchical) 0 else 10_000) + sourceSegments * 2

    fun matches(packageName: String): Boolean {
        return if (hierarchical) {
            source.isEmpty() || packageName == source || packageName.startsWith(sourcePrefix)
        } else {
            packageName == source
        }
    }

    fun map(originalPackage: String): String {
        return if (hierarchical) {
            val suffix = when {
                source.isEmpty() -> originalPackage
                originalPackage.length == source.length -> ""
                else -> originalPackage.substring(source.length + 1)
            }
            when {
                target.isEmpty() -> suffix
                suffix.isEmpty() -> target
                else -> "$target.$suffix"
            }
        } else {
            target
        }
    }
}

private data class PrefixRule(
    val packageName: String,
    val prefix: String,
    val hierarchical: Boolean
) {
    private val packagePrefix = if (packageName.isEmpty()) "" else "$packageName."
    private val packageSegments = if (packageName.isBlank()) 0 else packageName.split('.').size
    val specificity: Int = (if (hierarchical) 0 else 10_000) + packageSegments * 2

    fun matches(candidatePackage: String): Boolean {
        return if (hierarchical) {
            packageName.isEmpty() || candidatePackage == packageName || candidatePackage.startsWith(packagePrefix)
        } else {
            candidatePackage == packageName
        }
    }

    fun strip(simpleName: String): String {
        if (prefix.isEmpty() || !simpleName.startsWith(prefix)) {
            return simpleName
        }
        val dollarIndex = simpleName.indexOf('$')
        val topLevel = if (dollarIndex >= 0) simpleName.substring(0, dollarIndex) else simpleName
        if (!topLevel.startsWith(prefix)) {
            return simpleName
        }
        val strippedTopLevel = topLevel.removePrefix(prefix)
        return if (dollarIndex >= 0) {
            strippedTopLevel + simpleName.substring(dollarIndex)
        } else {
            strippedTopLevel
        }
    }
}
