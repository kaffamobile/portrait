package tech.kaffa.portrait.codegen

import io.github.classgraph.ArrayTypeSignature
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.ClassRefTypeSignature
import io.github.classgraph.HierarchicalTypeSignature
import io.github.classgraph.MethodInfo
import io.github.classgraph.MethodParameterInfo
import io.github.classgraph.MethodTypeSignature
import io.github.classgraph.ScanResult
import io.github.classgraph.TypeArgument
import io.github.classgraph.TypeVariableSignature
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import tech.kaffa.portrait.Includes
import tech.kaffa.portrait.codegen.utils.ClassGraphLocator
import java.util.*

class ClasspathScanner(private val classpath: String) {
    companion object {
        private val logger = LoggerFactory.getLogger(ClasspathScanner::class.java)
        private val PRIMITIVE_NAMES = setOf(
            "boolean",
            "byte",
            "char",
            "short",
            "int",
            "long",
            "float",
            "double",
            "void"
        )
    }

    private lateinit var scanResult: ScanResult
    private lateinit var classFileLocator: ClassFileLocator
    private lateinit var typePool: TypePool
    private lateinit var reflectives: MutableSet<ClassInfo>
    private lateinit var proxyTargets: MutableSet<ClassInfo>
    private lateinit var extraReflectiveNames: LinkedHashSet<String>
    private lateinit var extraProxyTargetNames: LinkedHashSet<String>

    data class Result(
        val proxyTargets: Set<ClassInfo>,
        val reflectives: Set<ClassInfo>,
        val proxyTargetClassNames: Set<String>,
        val reflectiveClassNames: Set<String>,
        val typePool: TypePool,
        private val classFileLocator: ClassFileLocator,
        val result: ScanResult
    ) : AutoCloseable {
        override fun close() {
            try {
                classFileLocator.close()
            } catch (ignored: Exception) {
                logger.debug("Failed to close ClassFileLocator: ${ignored.message}")
            }
            try {
                result.close()
            } catch (ignored: Exception) {
                logger.debug("Failed to close ScanResult: ${ignored.message}")
            }
        }
    }

    fun scan(): Result {
        initializeState()

        collectReflectives()
        collectProxyTargets()
        collectIncludeAnnotations()

        val reflectiveNames = LinkedHashSet<String>().apply {
            reflectives.mapTo(this) { it.name }
            addAll(extraReflectiveNames)
        }

        val proxyTargetNames = LinkedHashSet<String>().apply {
            proxyTargets.mapTo(this) { it.name }
            addAll(extraProxyTargetNames)
        }

        return Result(
            proxyTargets.toSet(),
            reflectives.toSet(),
            proxyTargetNames,
            reflectiveNames,
            typePool,
            classFileLocator,
            scanResult
        )
    }

    private fun initializeState() {
        val classGraph = if (classpath.isBlank()) {
            ClassGraph().enableAllInfo()
        } else {
            ClassGraph().overrideClasspath(classpath).enableAllInfo()
        }

        scanResult = classGraph.scan()
        classFileLocator = ClassFileLocator.Compound(
            ClassGraphLocator(scanResult),
            ClassFileLocator.ForClassLoader.ofSystemLoader()
        )
        typePool = TypePool.Default.of(classFileLocator)
        reflectives = mutableSetOf()
        proxyTargets = mutableSetOf()
        extraReflectiveNames = LinkedHashSet()
        extraProxyTargetNames = LinkedHashSet()
    }

    private fun collectIncludeAnnotations() {
        val additionalReflectives = mutableSetOf<ClassInfo>()
        val additionalProxyTargets = mutableSetOf<ClassInfo>()

        for (classInfo in scanResult.getClassesWithAnnotation("tech.kaffa.portrait.Reflective\$Include")) {
            val annotations = classInfo.getAnnotationInfoRepeatable("tech.kaffa.portrait.Reflective\$Include")
            for (annotation in annotations) {
                val classesArray = annotation?.parameterValues?.get("classes")?.value as? Array<*> ?: continue
                val includes = extractIncludes(annotation.parameterValues?.get("including")?.value)

                classesArray.forEach { classRef ->
                    val className = classRef.toString().removeSuffix(".class")
                    val targetClass = scanResult.getClassInfo(className)
                    if (targetClass != null) {
                        additionalReflectives.add(targetClass)
                        reflectives.add(targetClass)
                        applyReflectiveIncludes(targetClass, includes)
                    } else {
                        extraReflectiveNames.add(className)
                        applyReflectiveIncludesForName(className, includes)
                    }
                }
            }
        }

        for (classInfo in scanResult.getClassesWithAnnotation("tech.kaffa.portrait.ProxyTarget\$Include")) {
            val annotations = classInfo.getAnnotationInfoRepeatable("tech.kaffa.portrait.ProxyTarget\$Include")
            for (annotation in annotations) {
                val classesArray = annotation?.parameterValues?.get("classes")?.value as? Array<*> ?: continue
                val includes = extractIncludes(annotation.parameterValues?.get("including")?.value)

                classesArray.forEach { classRef ->
                    val className = classRef.toString().removeSuffix(".class")
                    val targetClass = scanResult.getClassInfo(className)
                    if (targetClass != null) {
                        additionalProxyTargets.add(targetClass)
                        reflectives.add(targetClass)
                        proxyTargets.add(targetClass)
                        applyProxyIncludes(targetClass, includes)
                    } else {
                        extraProxyTargetNames.add(className)
                        extraReflectiveNames.add(className)
                        applyProxyIncludesForName(className, includes)
                    }
                }
            }
        }
        reflectives += additionalReflectives
        proxyTargets += additionalProxyTargets
    }

    private fun collectReflectives() {
        for (classInfo in scanResult.getClassesWithAnnotation("tech.kaffa.portrait.Reflective")) {
            reflectives.add(classInfo)
            val annotation = classInfo.getAnnotationInfo("tech.kaffa.portrait.Reflective")
            val includes = extractIncludes(annotation?.parameterValues?.get("including")?.value)
            applyReflectiveIncludes(classInfo, includes)
        }
    }

    private fun collectProxyTargets() {
        for (classInfo in scanResult.getClassesWithAnnotation("tech.kaffa.portrait.ProxyTarget")) {
            proxyTargets.add(classInfo)
            val annotation = classInfo.getAnnotationInfo("tech.kaffa.portrait.ProxyTarget")
            val includes = extractIncludes(annotation?.parameterValues?.get("including")?.value)
            applyProxyIncludes(classInfo, includes)
        }
    }

    private fun extractIncludes(raw: Any?): EnumSet<Includes> {
        val includes = EnumSet.noneOf(Includes::class.java)
        val values = raw as? Array<*> ?: return includes
        values.forEach { value ->
            val name = value?.toString()?.substringAfterLast('.') ?: return@forEach
            runCatching { Includes.valueOf(name) }
                .getOrNull()
                ?.let { includes.add(it) }
        }
        return includes
    }

    private fun applyReflectiveIncludes(
        targetClass: ClassInfo,
        includes: EnumSet<Includes>
    ) {
        if (includes.isEmpty()) return

        val targetType = resolveType(targetClass.name)

        if (includes.contains(Includes.DIRECT_SUBTYPES)) {
            reflectives.addAll(targetClass.subclasses)
            if (targetClass.isInterface) {
                reflectives.addAll(targetClass.classesImplementing)
            }
        }

        if (includes.contains(Includes.ALL_SUBTYPES)) {
            addAllSubtypes(targetClass)
        }

        if (includes.contains(Includes.DIRECT_SUPERTYPES)) {
            targetClass.superclass?.let { reflectives.add(it) }
            reflectives.addAll(targetClass.interfaces)
            addDirectSupertypes(targetType)
        }

        if (includes.contains(Includes.ALL_SUPERTYPES)) {
            addAllSupertypes(targetClass)
            val added = addSupertypesViaTypePool(targetClass.name, extraReflectiveNames)
            added.mapNotNull { scanResult.getClassInfo(it) }.forEach(reflectives::add)
        }

        handlePublicApiIncludes(targetClass, targetType, includes)
    }

    private fun applyProxyIncludes(
        targetClass: ClassInfo,
        includes: EnumSet<Includes>
    ) {
        if (includes.isEmpty()) return

        val targetType = resolveType(targetClass.name)

        if (includes.contains(Includes.DIRECT_SUBTYPES)) {
            val subInterfaces = targetClass.subclasses.filter { it.isInterface }
            proxyTargets.addAll(subInterfaces)
            reflectives.addAll(subInterfaces)
        }

        if (includes.contains(Includes.ALL_SUBTYPES)) {
            addAllSubinterfaces(targetClass)
        }

        if (includes.contains(Includes.DIRECT_SUPERTYPES)) {
            val superInterfaces = targetClass.interfaces
            proxyTargets.addAll(superInterfaces)
            reflectives.addAll(superInterfaces)
            addDirectSuperinterfaces(targetType)
        }

        if (includes.contains(Includes.ALL_SUPERTYPES)) {
            addAllSuperinterfaces(targetClass)
            val added = addSupertypesViaTypePool(targetClass.name, extraProxyTargetNames)
            added.forEach { name ->
                extraReflectiveNames.add(name)
                val classInfo = scanResult.getClassInfo(name)
                if (classInfo != null) {
                    reflectives.add(classInfo)
                    if (classInfo.isInterface) {
                        proxyTargets.add(classInfo)
                    }
                } else if (isInterface(name)) {
                    extraProxyTargetNames.add(name)
                }
            }
        }

        val discoveredNames = handlePublicApiIncludes(targetClass, targetType, includes)

        discoveredNames.forEach { name ->
            val classInfo = scanResult.getClassInfo(name)
            if (classInfo?.isInterface == true) {
                proxyTargets.add(classInfo)
            } else if (classInfo == null && isInterface(name)) {
                extraProxyTargetNames.add(name)
            }
        }
    }

    private fun handlePublicApiIncludes(
        targetClass: ClassInfo?,
        targetType: TypeDescription?,
        includes: EnumSet<Includes>
    ): Set<String> {
        if (!includes.includesPublicApi()) return emptySet()

        val publicApiInfos = mutableSetOf<ClassInfo>()
        val publicApiNames = mutableSetOf<String>()

        collectPublicApiTypes(targetClass, targetType, publicApiInfos, publicApiNames)

        if (publicApiInfos.isEmpty() && publicApiNames.isEmpty()) return emptySet()

        val discoveredNames = LinkedHashSet<String>()

        publicApiInfos.forEach {
            reflectives.add(it)
            discoveredNames.add(it.name)
        }

        publicApiNames.forEach {
            extraReflectiveNames.add(it)
            discoveredNames.add(it)
        }

        if (includes.contains(Includes.PUBLIC_API_SUPERTYPES)) {
            publicApiInfos.forEach { addAllSupertypes(it) }
            publicApiNames.forEach { name ->
                val added = addSupertypesViaTypePool(name, extraReflectiveNames)
                added.mapNotNull { scanResult.getClassInfo(it) }.forEach(reflectives::add)
                discoveredNames.addAll(added)
            }
        }

        if (includes.contains(Includes.PUBLIC_API_SUBTYPES)) {
            publicApiInfos.forEach { addAllSubtypes(it) }
        }

        return discoveredNames
    }

    private fun collectPublicApiTypes(
        targetClass: ClassInfo?,
        targetType: TypeDescription?,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>
    ) {
        targetClass?.let {
            collectPublicApiTypesFromClassInfo(it, collectedInfos, collectedNames)
        }

        val effectiveType = targetType ?: targetClass?.name?.let { resolveType(it) }
        effectiveType?.let {
            collectPublicApiTypesFromType(it, collectedInfos, collectedNames)
        }
    }

    private fun collectPublicApiTypesFromClassInfo(
        classInfo: ClassInfo,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>
    ) {
        classInfo.fieldInfo
            .filter { it.isPublic }
            .forEach { field ->
                collectTypeClassInfos(field.typeSignatureOrTypeDescriptor, collectedInfos, collectedNames)
            }

        classInfo.methodInfo.forEach { method: MethodInfo ->
            if (!method.isPublic) return@forEach

            val signature = method.typeSignatureOrTypeDescriptor ?: return@forEach

            if (!method.isConstructor) {
                collectTypeClassInfos(signature.resultType, collectedInfos, collectedNames)
            }

            method.parameterInfo?.forEach { parameter: MethodParameterInfo ->
                collectTypeClassInfos(parameter.typeSignatureOrTypeDescriptor, collectedInfos, collectedNames)
            }
        }
    }

    private fun collectPublicApiTypesFromType(
        type: TypeDescription,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>,
        visited: MutableSet<String> = mutableSetOf()
    ) {
        type.declaredFields
            .filter { it.isPublic }
            .forEach { field ->
                collectFromGeneric(field.type, collectedInfos, collectedNames, visited)
            }

        type.declaredMethods
            .filter { it.isPublic }
            .forEach { method ->
                if (!method.isConstructor) {
                    collectFromGeneric(method.returnType, collectedInfos, collectedNames, visited)
                }
                method.parameters.forEach { parameter ->
                    collectFromGeneric(parameter.type, collectedInfos, collectedNames, visited)
                }
                method.exceptionTypes.forEach {
                    collectFromGeneric(it, collectedInfos, collectedNames, visited)
                }
            }
    }

    private fun collectFromGeneric(
        generic: TypeDescription.Generic?,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>,
        visited: MutableSet<String>
    ) {
        if (generic == null) return
        val erasure = generic.asErasure()

        if (erasure.isPrimitive || erasure.represents(Void.TYPE)) return

        if (erasure.isArray) {
            collectFromGeneric(generic.componentType, collectedInfos, collectedNames, visited)
            return
        }

        val normalized = normalizeTypeName(erasure.typeName) ?: return
        if (!visited.add(normalized)) return

        scanResult.getClassInfo(normalized)?.let {
            collectedInfos.add(it)
        } ?: collectedNames.add(normalized)

        runCatching { generic.typeArguments }
            .getOrNull()
            ?.forEach {
                collectFromGeneric(it, collectedInfos, collectedNames, visited)
            }
        runCatching { generic.upperBounds }
            .getOrNull()
            ?.forEach {
                collectFromGeneric(it, collectedInfos, collectedNames, visited)
            }
        runCatching { generic.lowerBounds }
            .getOrNull()
            ?.forEach {
                collectFromGeneric(it, collectedInfos, collectedNames, visited)
            }
    }

    private fun collectTypeClassInfos(
        signature: HierarchicalTypeSignature?,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>
    ) {
        when (signature) {
            null -> return
            is ClassRefTypeSignature -> {
                val className = normalizeTypeName(signature.fullyQualifiedClassName)
                if (className != null) {
                    val classInfo = signature.classInfo ?: scanResult.getClassInfo(className)
                    if (classInfo != null) {
                        collectedInfos.add(classInfo)
                    } else {
                        collectedNames.add(className)
                    }
                }
                signature.typeArguments?.forEach {
                    collectTypeClassInfos(it, collectedInfos, collectedNames)
                }
                signature.suffixTypeArguments?.forEach { args ->
                    args.forEach {
                        collectTypeClassInfos(it, collectedInfos, collectedNames)
                    }
                }
            }

            is ArrayTypeSignature -> {
                collectTypeClassInfos(signature.elementTypeSignature, collectedInfos, collectedNames)
            }

            is TypeArgument -> {
                collectTypeClassInfos(signature.typeSignature, collectedInfos, collectedNames)
            }

            is TypeVariableSignature -> {
                signature.resolve()?.let { typeParameter ->
                    typeParameter.classBound?.let {
                        collectTypeClassInfos(it, collectedInfos, collectedNames)
                    }
                    typeParameter.interfaceBounds?.forEach {
                        collectTypeClassInfos(it, collectedInfos, collectedNames)
                    }
                }
            }

            is MethodTypeSignature -> {
                collectTypeClassInfos(signature.resultType, collectedInfos, collectedNames)
                signature.throwsSignatures?.forEach {
                    collectTypeClassInfos(it, collectedInfos, collectedNames)
                }
            }
        }
    }

    private fun addDirectSupertypes(targetType: TypeDescription?) {
        if (targetType == null) return

        targetType.superClass?.let { superType ->
            val name = normalizeTypeName(superType.asErasure().typeName) ?: return@let
            scanResult.getClassInfo(name)?.let(reflectives::add) ?: extraReflectiveNames.add(name)
        }

        targetType.interfaces.forEach { iface ->
            val name = normalizeTypeName(iface.asErasure().typeName) ?: return@forEach
            scanResult.getClassInfo(name)?.let(reflectives::add) ?: extraReflectiveNames.add(name)
        }
    }

    private fun addDirectSuperinterfaces(targetType: TypeDescription?) {
        if (targetType == null) return

        targetType.interfaces.forEach { iface ->
            val name = normalizeTypeName(iface.asErasure().typeName) ?: return@forEach
            val classInfo = scanResult.getClassInfo(name)
            if (classInfo != null) {
                proxyTargets.add(classInfo)
            } else {
                extraProxyTargetNames.add(name)
            }
            extraReflectiveNames.add(name)
        }
    }

    private fun addSupertypesViaTypePool(
        className: String,
        extraNames: MutableSet<String>,
        visited: MutableSet<String> = mutableSetOf()
    ): Set<String> {
        val normalized = normalizeTypeName(className) ?: return emptySet()
        if (!visited.add(normalized)) return emptySet()

        val type = resolveType(normalized) ?: return emptySet()
        val discovered = LinkedHashSet<String>()

        type.superClass?.let { superType ->
            val name = normalizeTypeName(superType.asErasure().typeName)
            if (name != null && name != normalized) {
                discovered.add(name)
                if (scanResult.getClassInfo(name) == null) {
                    extraNames.add(name)
                }
                discovered.addAll(addSupertypesViaTypePool(name, extraNames, visited))
            }
        }

        type.interfaces.forEach { iface ->
            val name = normalizeTypeName(iface.asErasure().typeName)
            if (name != null && name != normalized) {
                discovered.add(name)
                if (scanResult.getClassInfo(name) == null) {
                    extraNames.add(name)
                }
                discovered.addAll(addSupertypesViaTypePool(name, extraNames, visited))
            }
        }

        return discovered
    }

    private fun applyReflectiveIncludesForName(
        className: String,
        includes: EnumSet<Includes>
    ) {
        if (includes.isEmpty()) return

        val targetType = resolveType(className)

        if (includes.contains(Includes.DIRECT_SUPERTYPES)) {
            addDirectSupertypes(targetType)
        }

        if (includes.contains(Includes.ALL_SUPERTYPES)) {
            val added = addSupertypesViaTypePool(className, extraReflectiveNames)
            added.mapNotNull { scanResult.getClassInfo(it) }.forEach(reflectives::add)
        }

        handlePublicApiIncludes(null, targetType, includes)
    }

    private fun applyProxyIncludesForName(
        className: String,
        includes: EnumSet<Includes>
    ) {
        if (includes.isEmpty()) return

        val targetType = resolveType(className)

        if (includes.contains(Includes.DIRECT_SUPERTYPES)) {
            addDirectSuperinterfaces(targetType)
        }

        if (includes.contains(Includes.ALL_SUPERTYPES)) {
            val added = addSupertypesViaTypePool(className, extraProxyTargetNames)
            added.forEach { name ->
                extraReflectiveNames.add(name)
                val classInfo = scanResult.getClassInfo(name)
                if (classInfo != null) {
                    reflectives.add(classInfo)
                    if (classInfo.isInterface) {
                        proxyTargets.add(classInfo)
                    }
                } else if (isInterface(name)) {
                    extraProxyTargetNames.add(name)
                }
            }
        }

        val discovered = handlePublicApiIncludes(null, targetType, includes)

        discovered.forEach { name ->
            val classInfo = scanResult.getClassInfo(name)
            if (classInfo?.isInterface == true) {
                proxyTargets.add(classInfo)
            } else if (classInfo == null && isInterface(name)) {
                extraProxyTargetNames.add(name)
            }
        }
    }

    private fun EnumSet<Includes>.includesPublicApi(): Boolean {
        return contains(Includes.PUBLIC_API) ||
                contains(Includes.PUBLIC_API_SUBTYPES) ||
                contains(Includes.PUBLIC_API_SUPERTYPES)
    }

    private fun addAllSubtypes(classInfo: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()

        fun addSubtypesRecursive(current: ClassInfo) {
            if (!visited.add(current)) return

            current.subclasses.forEach { subclass ->
                reflectives.add(subclass)
                addSubtypesRecursive(subclass)
            }

            if (current.isInterface) {
                current.classesImplementing.forEach { implementer ->
                    reflectives.add(implementer)
                    addSubtypesRecursive(implementer)
                }
            }
        }

        addSubtypesRecursive(classInfo)
    }

    private fun addAllSupertypes(classInfo: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()

        fun addSupertypesRecursive(current: ClassInfo) {
            if (!visited.add(current)) return

            current.superclass?.let { superclass ->
                reflectives.add(superclass)
                addSupertypesRecursive(superclass)
            }

            current.interfaces.forEach { iface ->
                reflectives.add(iface)
                addSupertypesRecursive(iface)
            }
        }

        addSupertypesRecursive(classInfo)
    }

    private fun addAllSubinterfaces(classInfo: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()

        fun addSubinterfacesRecursive(current: ClassInfo) {
            if (!current.isInterface || !visited.add(current)) return

            current.subclasses.filter { it.isInterface }.forEach { subinterface ->
                proxyTargets.add(subinterface)
                reflectives.add(subinterface)
                addSubinterfacesRecursive(subinterface)
            }
        }

        addSubinterfacesRecursive(classInfo)
    }

    private fun addAllSuperinterfaces(classInfo: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()

        fun addSuperinterfacesRecursive(current: ClassInfo) {
            if (!visited.add(current)) return

            current.interfaces.forEach { iface ->
                proxyTargets.add(iface)
                reflectives.add(iface)
                addSuperinterfacesRecursive(iface)
            }
        }

        addSuperinterfacesRecursive(classInfo)
    }

    private fun resolveType(className: String): TypeDescription? {
        val normalized = normalizeTypeName(className) ?: return null
        return runCatching {
            typePool.describe(normalized).resolve()
        }.onFailure {
            logger.debug("Failed to resolve type '$normalized': ${it.message}")
        }.getOrNull()
    }

    private fun isInterface(className: String): Boolean {
        return resolveType(className)?.isInterface == true
    }

    private fun normalizeTypeName(typeName: String?): String? {
        val nonNullName = typeName ?: return null
        if (nonNullName.isBlank()) return null
        var normalized = nonNullName
        while (normalized.endsWith("[]")) {
            normalized = normalized.removeSuffix("[]")
        }
        if (normalized in PRIMITIVE_NAMES || normalized.isEmpty()) return null
        return normalized
    }
}
