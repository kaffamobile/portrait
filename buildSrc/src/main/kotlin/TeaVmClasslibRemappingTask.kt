package tech.kaffa.portrait.codegen.build

import java.io.File
import java.nio.file.Files
import java.util.Properties
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.Deflater
import javax.inject.Inject
import kotlin.io.path.isRegularFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InvokeDynamicInsnNode

/**
 * Gradle task that remaps TeaVM classlib classes according to mapping rules defined in `META-INF/teavm.properties` files.
 *
 * This task processes JAR files and directories containing compiled classes, applies package/class name remapping,
 * and handles TeaVM-specific annotations to produce a single remapped JAR output.
 *
 * ## TeaVM Annotations Processed
 * - `@Superclass`: Overrides the superclass of a class
 * - `@Rename`: Renames methods
 * - `@Remove`: Removes methods from the output
 *
 * ## Mapping Rules
 * The task reads mapping rules from `META-INF/teavm.properties` files found in input JARs or directories.
 * Supported directives:
 * - `mapClass|original.ClassName=new.ClassName`: Maps a specific class
 * - `mapPackage|original.package=new.package`: Maps a single package (non-hierarchical)
 * - `mapPackageHierarchy|original.package=new.package`: Maps a package and all subpackages
 * - `stripPrefixFromPackageClasses|package.name=Prefix`: Strips prefix from class names in a package
 * - `stripPrefixFromPackageHierarchyClasses|package.name=Prefix`: Strips prefix from class names in a package hierarchy
 *
 * ## Example
 * ```kotlin
 * tasks.register<TeavmClasslibRemappedJar>("remapTeaVm") {
 *     sources.from(configurations.named("teavmClasslib"))
 * }
 * ```
 *
 * @property sources Input files (JARs or directories) containing classes to be remapped. Must be on the classpath.
 * @property outputJar The output JAR file containing all remapped classes. Defaults to `build/generated/teavm/teavm-classlib-remapped.jar`.
 */
@DisableCachingByDefault(because = "Task performs custom bytecode rewriting before archiving")
abstract class TeaVmClasslibRemappingTask @Inject constructor() : DefaultTask() {

    @get:InputFiles
    @get:Classpath
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    init {
        outputJar.convention(
            project.layout.buildDirectory.file("generated/teavm/teavm-classlib-remapped.jar")
        )
    }

    /**
     * Executes the remapping task by:
     * 1. Loading mapping rules from all `META-INF/teavm.properties` files
     * 2. Processing all classes from input sources
     * 3. Applying remapping and TeaVM annotations
     * 4. Writing remapped classes to the output JAR
     */
    @TaskAction
    fun execute() {
        val inputFiles = sources.files
        val outputFile = outputJar.get().asFile

        if (inputFiles.isEmpty()) {
            logger.warn("TeaVM remapping skipped: no files resolved for task $name.")
            return
        }

        outputFile.parentFile.mkdirs()

        val rules = loadMappingRules(inputFiles)
        val remapper = TeaVmClassRemapper(rules)
        val writtenClasses = mutableSetOf<String>()

        try {
            outputFile.outputStream().buffered().use { fileOut ->
                JarOutputStream(fileOut).use { jarOut ->
                    jarOut.setLevel(Deflater.DEFAULT_COMPRESSION)

                    inputFiles.forEach { file ->
                        processInputFile(remapper, file, jarOut, writtenClasses)
                    }
                }
            }

            if (writtenClasses.isEmpty()) {
                logger.warn("No classes were written to output JAR. Check that input sources contain valid class files.")
            } else {
                logger.info("Successfully remapped ${writtenClasses.size} classes to ${outputFile.name}")
            }
        } catch (e: Exception) {
            logger.error("Failed to generate remapped JAR", e)
            throw e
        }
    }

    /**
     * Loads and consolidates all mapping rules from `META-INF/teavm.properties` files in the input sources.
     *
     * @param inputFiles The collection of JAR files and directories to scan for properties files
     * @return A frozen [ClassMappingRules] instance containing all discovered mapping rules
     */
    private fun loadMappingRules(inputFiles: Set<File>): ClassMappingRules {
        val rules = ClassMappingRules()
        inputFiles.forEach { rules.loadFromSource(it) }
        rules.freeze()

        if (!rules.hasMappings()) {
            logger.warn(
                "TeaVM remapping found no META-INF/teavm.properties files in ${inputFiles.size} inputs."
            )
        }
        return rules
    }

    /**
     * Processes a single input file (JAR or directory) and writes remapped classes to the output JAR.
     *
     * @param remapper The remapper to use for class name transformations
     * @param file The input file to process
     * @param jarOut The output JAR stream to write to
     * @param writtenClasses Set tracking already-written class names to prevent duplicates
     */
    private fun processInputFile(
        remapper: TeaVmClassRemapper,
        file: File,
        jarOut: JarOutputStream,
        writtenClasses: MutableSet<String>
    ) {
        try {
            when {
                file.isDirectory -> processDirectory(remapper, file, jarOut, writtenClasses)
                file.extension.equals("jar", ignoreCase = true) -> processJarFile(
                    remapper,
                    file,
                    jarOut,
                    writtenClasses
                )

                else -> logger.debug("Skipping TeaVM remap input {} (unsupported type)", file)
            }
        } catch (e: Exception) {
            logger.error("Failed to process input file: $file", e)
            throw e
        }
    }

    /**
     * Processes all class files in a directory recursively.
     *
     * @param remapper The remapper to use for class name transformations
     * @param directory The directory to scan for class files
     * @param jarOut The output JAR stream to write to
     * @param writtenClasses Set tracking already-written class names to prevent duplicates
     */
    private fun processDirectory(
        remapper: TeaVmClassRemapper,
        directory: File,
        jarOut: JarOutputStream,
        writtenClasses: MutableSet<String>
    ) {
        Files.walk(directory.toPath()).use { paths ->
            paths.filter { it.isRegularFile() && it.fileName.toString().endsWith(".class") }
                .forEach { path ->
                    val bytes = Files.readAllBytes(path)
                    remapAndWriteClass(remapper, bytes, jarOut, writtenClasses)
                }
        }
    }

    /**
     * Processes all class files in a JAR archive.
     *
     * @param remapper The remapper to use for class name transformations
     * @param jarFile The JAR file to process
     * @param jarOut The output JAR stream to write to
     * @param writtenClasses Set tracking already-written class names to prevent duplicates
     */
    private fun processJarFile(
        remapper: TeaVmClassRemapper,
        jarFile: File,
        jarOut: JarOutputStream,
        writtenClasses: MutableSet<String>
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
                    remapAndWriteClass(remapper, bytes, jarOut, writtenClasses)
                }
            }
        }
    }

    /**
     * Remaps a single class and writes it to the output JAR.
     *
     * This method:
     * 1. Parses the class bytecode using ASM
     * 2. Applies TeaVM annotation overrides (superclass, method renames, removals)
     * 3. Remaps package and class names according to the mapping rules
     * 4. Writes the remapped class to the output JAR
     *
     * @param remapper The remapper to use for class name transformations
     * @param classBytes The original class bytecode
     * @param jarOut The output JAR stream to write to
     * @param writtenClasses Set tracking already-written class names to prevent duplicates
     */
    private fun remapAndWriteClass(
        remapper: TeaVmClassRemapper,
        classBytes: ByteArray,
        jarOut: JarOutputStream,
        writtenClasses: MutableSet<String>
    ) {
        val reader = ClassReader(classBytes)
        if (reader.className == "module-info") {
            return
        }

        val classNode = ClassNode()
        reader.accept(classNode, 0)

        applySuperclassAnnotation(classNode)
        applyMethodAnnotations(classNode)
        remapInvokeDynamic(classNode, remapper)

        val remappedInternalName = remapper.map(classNode.name) ?: classNode.name
        val writer = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val classRemapper = ClassRemapper(writer, remapper)
        classNode.accept(classRemapper)
        val remappedBytes = writer.toByteArray()

        val jarEntryName = "$remappedInternalName.class"
        if (!writtenClasses.add(jarEntryName)) {
            logger.warn("Skipping duplicate remapped class entry: $jarEntryName")
            return
        }

        val jarEntry = JarEntry(jarEntryName)
        jarEntry.time = REPRODUCIBLE_BUILD_TIMESTAMP
        jarOut.putNextEntry(jarEntry)
        jarOut.write(remappedBytes)
        jarOut.closeEntry()
    }

    companion object {
        /**
         * Timestamp used for JAR entries to ensure reproducible builds.
         * Set to 0 (Unix epoch) to make builds deterministic.
         */
        private const val REPRODUCIBLE_BUILD_TIMESTAMP = 0L


        /**
         * Applies the `@Superclass` annotation if present, overriding the class's superclass.
         *
         * @param classNode The ASM ClassNode to modify
         */
        private fun applySuperclassAnnotation(classNode: ClassNode) {
            val annotation = (classNode.visibleAnnotations.orEmpty() + classNode.invisibleAnnotations.orEmpty())
                .firstOrNull { it.desc == TEAVM_SUPERCLASS_ANNOTATION }
                ?: return

            val overrideSuperclass = annotation.extractSuperclassValue() ?: return
            val internalName = overrideSuperclass.ifEmpty { null }
            classNode.superName = internalName?.takeUnless { it == classNode.name }
        }

        /**
         * Applies method-level TeaVM annotations (`@Rename`, `@Remove`) to all methods in the class.
         *
         * Methods marked with `@Remove` are excluded from the output.
         * Methods marked with `@Rename` have their names changed.
         *
         * @param classNode The ASM ClassNode to modify
         */
        private fun applyMethodAnnotations(classNode: ClassNode) {
            if (classNode.methods.isNullOrEmpty()) {
                return
            }

            val methodsBySignature = linkedMapOf<String, MethodNode>()
            for (method in classNode.methods) {
                val annotations = method.collectAnnotations()

                // Skip methods marked for removal
                if (annotations.any { it.desc == TEAVM_REMOVE_ANNOTATION }) {
                    continue
                }

                // Apply rename annotation if present
                val renameAnnotation = annotations.firstOrNull { it.desc == TEAVM_RENAME_ANNOTATION }
                val newName = renameAnnotation?.extractRenameValue()
                if (renameAnnotation != null) {
                    method.removeAnnotationByDescriptor(TEAVM_RENAME_ANNOTATION)
                }
                if (!newName.isNullOrEmpty()) {
                    method.name = newName
                }

                val signature = method.name + method.desc
                methodsBySignature[signature] = method
            }
            classNode.methods = methodsBySignature.values.toMutableList()
        }

        /**
         * Collects all annotations (visible and invisible) from a method.
         *
         * @return A list of all annotations on this method
         */
        private fun MethodNode.collectAnnotations(): List<AnnotationNode> {
            val visible = visibleAnnotations.orEmpty()
            val invisible = invisibleAnnotations.orEmpty()
            return when {
                visible.isEmpty() -> invisible
                invisible.isEmpty() -> visible
                else -> visible + invisible
            }
        }

        /**
         * Removes all annotations with the specified descriptor from this method.
         *
         * @param descriptor The annotation descriptor to remove (e.g., `Lorg/teavm/interop/Rename;`)
         */
        private fun MethodNode.removeAnnotationByDescriptor(descriptor: String) {
            visibleAnnotations = visibleAnnotations.filterOutAnnotation(descriptor)
            invisibleAnnotations = invisibleAnnotations.filterOutAnnotation(descriptor)
        }

        /**
         * Filters out annotations with the specified descriptor from a list.
         *
         * @param descriptor The annotation descriptor to remove
         * @return A new mutable list without the specified annotation, or null if the result is empty
         */
        private fun MutableList<AnnotationNode>?.filterOutAnnotation(descriptor: String): MutableList<AnnotationNode>? {
            if (this == null) {
                return null
            }
            val filtered = this.filter { it.desc != descriptor }
            return if (filtered.isEmpty()) null else filtered.toMutableList()
        }

        /**
         * Extracts the superclass value from a `@Superclass` annotation.
         *
         * @return The internal class name of the superclass, empty string for no superclass, or null if not found
         */
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

        /**
         * Extracts the rename value from a `@Rename` annotation.
         *
         * @return The new method name, or null if not found
         */
        private fun AnnotationNode.extractRenameValue(): String? {
            val values = this.values ?: return null
            val keyIndex = values.indexOf("value")
            if (keyIndex < 0 || keyIndex + 1 >= values.size) {
                return null
            }
            return values[keyIndex + 1] as? String
        }

        /**
         * Remaps all class references inside invokedynamic instructions.
         * ClassRemapper doesn't handle these properly, so we need to do it manually.
         */
        private fun remapInvokeDynamic(classNode: ClassNode, remapper: Remapper) {
            for (method in classNode.methods) {
                val instructions = method.instructions ?: continue

                for (insn in instructions) {
                    if (insn is InvokeDynamicInsnNode) {
                        // Remap the descriptor
                        insn.desc = remapper.mapMethodDesc(insn.desc)

                        // Remap the bootstrap method handle
                        insn.bsm = remapHandle(insn.bsm, remapper)

                        // Remap bootstrap method arguments
                        insn.bsmArgs = insn.bsmArgs.map { arg ->
                            when (arg) {
                                is Type -> remapType(arg, remapper)
                                is Handle -> remapHandle(arg, remapper)
                                else -> arg
                            }
                        }.toTypedArray()
                    }
                }
            }
        }

        /**
         * Remaps a Handle (method or field reference).
         */
        private fun remapHandle(handle: Handle, remapper: Remapper): Handle {
            val owner = remapper.mapType(handle.owner)
            val name = when (handle.tag) {
                Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC,
                Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> {
                    remapper.mapFieldName(handle.owner, handle.name, handle.desc)
                }

                else -> {
                    remapper.mapMethodName(handle.owner, handle.name, handle.desc)
                }
            }
            val desc = when (handle.tag) {
                Opcodes.H_GETFIELD, Opcodes.H_GETSTATIC,
                Opcodes.H_PUTFIELD, Opcodes.H_PUTSTATIC -> {
                    remapper.mapDesc(handle.desc)
                }

                else -> {
                    remapper.mapMethodDesc(handle.desc)
                }
            }

            return Handle(
                handle.tag,
                owner,
                name,
                desc,
                handle.isInterface
            )
        }

        /**
         * Remaps a Type.
         */
        private fun remapType(type: Type, remapper: Remapper): Type {
            return when (type.sort) {
                Type.OBJECT -> Type.getObjectType(remapper.mapType(type.internalName))
                Type.ARRAY -> Type.getType(remapper.mapDesc(type.descriptor))
                Type.METHOD -> Type.getMethodType(remapper.mapMethodDesc(type.descriptor))
                else -> type
            }
        }
    }
}

/**
 * ASM [Remapper] implementation that applies TeaVM class and package name mappings.
 *
 * @property rules The mapping rules to apply during remapping
 */
private class TeaVmClassRemapper(
    private val rules: ClassMappingRules
) : Remapper() {

    /**
     * Maps an internal class name (e.g., `java/lang/String`) to its remapped form.
     *
     * @param internalName The internal class name to map, or null
     * @return The mapped internal class name, or null if the input was null
     */
    override fun map(internalName: String?): String? {
        if (internalName == null) {
            return null
        }
        return rules.mapInternalClassName(internalName)
    }

    /**
     * Maps a package name to its remapped form.
     *
     * @param name The package name to map (with `/` separators), or null
     * @return The mapped package name (with `/` separators), or null if the input was null
     */
    override fun mapPackageName(name: String?): String? {
        if (name == null) {
            return null
        }
        val mapped = rules.mapPackageName(name.replace('/', '.'))
        return mapped.replace('.', '/')
    }
}

/** TeaVM annotation descriptor for overriding a class's superclass */
private const val TEAVM_SUPERCLASS_ANNOTATION = "Lorg/teavm/interop/Superclass;"

/** TeaVM annotation descriptor for renaming methods */
private const val TEAVM_RENAME_ANNOTATION = "Lorg/teavm/interop/Rename;"

/** TeaVM annotation descriptor for removing methods from the output */
private const val TEAVM_REMOVE_ANNOTATION = "Lorg/teavm/interop/Remove;"

/** Path to TeaVM properties file within JARs and directories */
private const val TEAVM_PROPERTIES_PATH = "META-INF/teavm.properties"

/**
 * Manages class and package mapping rules loaded from TeaVM properties files.
 *
 * Rules are loaded from `META-INF/teavm.properties` files using these directives:
 * - `mapClass|original.ClassName=new.ClassName`
 * - `mapPackage|original.package=new.package`
 * - `mapPackageHierarchy|original.package=new.package`
 * - `stripPrefixFromPackageClasses|package.name=Prefix`
 * - `stripPrefixFromPackageHierarchyClasses|package.name=Prefix`
 *
 * After loading all rules, call [freeze] to prepare the rules for efficient lookup.
 */
private class ClassMappingRules {
    private val classMappings = mutableMapOf<String, String>()
    private val packageRules = mutableListOf<PackageRule>()
    private val prefixRules = mutableListOf<PrefixRule>()

    /**
     * Loads TeaVM properties from a source file (JAR or directory).
     *
     * @param source A JAR file or directory that may contain `META-INF/teavm.properties`
     */
    fun loadFromSource(source: File) {
        if (source.isDirectory) {
            val propertiesFile = File(source, TEAVM_PROPERTIES_PATH)
            if (propertiesFile.isFile) {
                propertiesFile.inputStream().use { input ->
                    val properties = Properties()
                    properties.load(input)
                    loadFromProperties(properties)
                }
            }
        } else if (source.extension.equals("jar", ignoreCase = true)) {
            JarFile(source).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.name == TEAVM_PROPERTIES_PATH) {
                        jar.getInputStream(entry).use { input ->
                            val properties = Properties()
                            properties.load(input)
                            loadFromProperties(properties)
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if any mapping rules have been loaded.
     *
     * @return `true` if at least one mapping rule exists
     */
    fun hasMappings(): Boolean =
        classMappings.isNotEmpty() || packageRules.isNotEmpty() || prefixRules.isNotEmpty()

    /**
     * Prepares the rules for efficient lookup by sorting them by specificity.
     *
     * Must be called after all rules are loaded and before any mapping operations.
     * More specific rules (exact package matches) are evaluated before less specific ones (hierarchical).
     */
    fun freeze() {
        packageRules.sortByDescending { it.specificity }
        prefixRules.sortByDescending { it.specificity }
    }

    /**
     * Maps an internal class name (e.g., `java/lang/String`) to its remapped form.
     *
     * The mapping process:
     * 1. Checks for an exact class mapping
     * 2. Applies package mapping to the class's package
     * 3. Strips any configured prefix from the simple class name
     * 4. Combines the mapped package with the stripped class name
     *
     * @param originalInternal The internal class name to map (with `/` separators)
     * @return The mapped internal class name (with `/` separators)
     */
    fun mapInternalClassName(originalInternal: String): String {
        val dottedClassName = originalInternal.replace('/', '.')

        // Check for exact class mapping first
        classMappings[dottedClassName]?.let { mapped ->
            return mapped.replace('.', '/')
        }

        // Split into package and simple name
        val lastDot = dottedClassName.lastIndexOf('.')
        val packageName = if (lastDot >= 0) dottedClassName.substring(0, lastDot) else ""
        val simpleName = if (lastDot >= 0) dottedClassName.substring(lastDot + 1) else dottedClassName

        // Apply package mapping and prefix stripping
        val strippedSimpleName = stripClassNamePrefix(packageName, simpleName)
        val mappedPackage = mapPackageName(packageName)

        // Reconstruct full class name
        val mappedClassName = if (mappedPackage.isEmpty()) {
            strippedSimpleName
        } else {
            "$mappedPackage.$strippedSimpleName"
        }
        return mappedClassName.replace('.', '/')
    }

    /**
     * Maps a package name to its remapped form.
     *
     * @param originalPackage The package name to map (with `.` separators)
     * @return The mapped package name (with `.` separators)
     */
    fun mapPackageName(originalPackage: String): String {
        val rule = packageRules.firstOrNull { it.matches(originalPackage) } ?: return originalPackage
        return rule.remap(originalPackage)
    }

    /**
     * Strips a configured prefix from a simple class name based on the class's package.
     *
     * @param packageName The package containing the class
     * @param simpleName The simple class name (without package)
     * @return The class name with prefix removed, or the original name if no rule applies
     */
    private fun stripClassNamePrefix(packageName: String, simpleName: String): String {
        val rule = prefixRules.firstOrNull { it.matches(packageName) } ?: return simpleName
        return rule.stripPrefix(simpleName)
    }

    /**
     * Parses and loads mapping rules from a Properties object.
     *
     * @param properties Properties loaded from a `teavm.properties` file
     */
    private fun loadFromProperties(properties: Properties) {
        for (entry in properties.entries) {
            val key = entry.key as String
            val value = entry.value as String
            val segments = key.split("|", limit = 2)
            val directive = segments[0]
            val subject = segments.getOrElse(1) { "" }

            when (directive) {
                DIRECTIVE_MAP_PACKAGE_HIERARCHY ->
                    packageRules += PackageRule(subject, value, hierarchical = true)
                DIRECTIVE_MAP_PACKAGE ->
                    packageRules += PackageRule(subject, value, hierarchical = false)
                DIRECTIVE_MAP_CLASS ->
                    classMappings[subject] = value
                DIRECTIVE_STRIP_PREFIX_HIERARCHY ->
                    prefixRules += PrefixRule(subject, value, hierarchical = true)
                DIRECTIVE_STRIP_PREFIX_PACKAGE ->
                    prefixRules += PrefixRule(subject, value, hierarchical = false)
            }
        }
    }

    companion object {
        private const val DIRECTIVE_MAP_CLASS = "mapClass"
        private const val DIRECTIVE_MAP_PACKAGE = "mapPackage"
        private const val DIRECTIVE_MAP_PACKAGE_HIERARCHY = "mapPackageHierarchy"
        private const val DIRECTIVE_STRIP_PREFIX_PACKAGE = "stripPrefixFromPackageClasses"
        private const val DIRECTIVE_STRIP_PREFIX_HIERARCHY = "stripPrefixFromPackageHierarchyClasses"
    }
}

/**
 * Represents a package mapping rule from TeaVM properties.
 *
 * @property sourcePackage The original package name (with `.` separators)
 * @property targetPackage The target package name (with `.` separators)
 * @property hierarchical If true, applies to subpackages; if false, only exact matches
 * @property specificity Numeric specificity for rule ordering (higher = more specific)
 */
private data class PackageRule(
    val sourcePackage: String,
    val targetPackage: String,
    val hierarchical: Boolean
) {
    private val sourcePrefix = if (sourcePackage.isEmpty()) "" else "$sourcePackage."
    private val sourceSegments = if (sourcePackage.isBlank()) 0 else sourcePackage.split('.').size

    /**
     * Specificity score for rule ordering.
     * Non-hierarchical rules get +10,000, then +2 per package segment for more specific matches.
     */
    val specificity: Int = (if (hierarchical) 0 else 10_000) + sourceSegments * 2

    /**
     * Checks if this rule matches the given package name.
     *
     * @param packageName The package name to test
     * @return `true` if this rule should be applied to the package
     */
    fun matches(packageName: String): Boolean {
        return if (hierarchical) {
            sourcePackage.isEmpty() || packageName == sourcePackage || packageName.startsWith(sourcePrefix)
        } else {
            packageName == sourcePackage
        }
    }

    /**
     * Remaps a package name according to this rule.
     *
     * For hierarchical rules, preserves the subpackage structure.
     * For non-hierarchical rules, replaces the entire package name.
     *
     * @param originalPackage The original package name
     * @return The remapped package name
     */
    fun remap(originalPackage: String): String {
        return if (hierarchical) {
            val suffix = when {
                sourcePackage.isEmpty() -> originalPackage
                originalPackage.length == sourcePackage.length -> ""
                else -> originalPackage.substring(sourcePackage.length + 1)
            }
            when {
                targetPackage.isEmpty() -> suffix
                suffix.isEmpty() -> targetPackage
                else -> "$targetPackage.$suffix"
            }
        } else {
            targetPackage
        }
    }
}

/**
 * Represents a class name prefix stripping rule from TeaVM properties.
 *
 * @property packageName The package this rule applies to (with `.` separators)
 * @property prefix The prefix to strip from class simple names
 * @property hierarchical If true, applies to subpackages; if false, only exact matches
 * @property specificity Numeric specificity for rule ordering (higher = more specific)
 */
private data class PrefixRule(
    val packageName: String,
    val prefix: String,
    val hierarchical: Boolean
) {
    private val packagePrefix = if (packageName.isEmpty()) "" else "$packageName."
    private val packageSegments = if (packageName.isBlank()) 0 else packageName.split('.').size

    /**
     * Specificity score for rule ordering.
     * Non-hierarchical rules get +10,000, then +2 per package segment for more specific matches.
     */
    val specificity: Int = (if (hierarchical) 0 else 10_000) + packageSegments * 2

    /**
     * Checks if this rule matches the given package name.
     *
     * @param candidatePackage The package name to test
     * @return `true` if this rule should be applied to classes in the package
     */
    fun matches(candidatePackage: String): Boolean {
        return if (hierarchical) {
            packageName.isEmpty() || candidatePackage == packageName || candidatePackage.startsWith(packagePrefix)
        } else {
            candidatePackage == packageName
        }
    }

    /**
     * Strips the configured prefix from a simple class name.
     *
     * Only strips from the top-level class name, preserving inner class names after `$`.
     *
     * @param simpleName The simple class name (no package)
     * @return The class name with prefix removed, or original if prefix doesn't match
     */
    fun stripPrefix(simpleName: String): String {
        if (prefix.isEmpty() || !simpleName.startsWith(prefix)) {
            return simpleName
        }

        val dollarIndex = simpleName.indexOf('$')
        val topLevelName = if (dollarIndex >= 0) simpleName.substring(0, dollarIndex) else simpleName

        if (!topLevelName.startsWith(prefix)) {
            return simpleName
        }

        val strippedTopLevel = topLevelName.removePrefix(prefix)
        return if (dollarIndex >= 0) {
            strippedTopLevel + simpleName.substring(dollarIndex)
        } else {
            strippedTopLevel
        }
    }
}