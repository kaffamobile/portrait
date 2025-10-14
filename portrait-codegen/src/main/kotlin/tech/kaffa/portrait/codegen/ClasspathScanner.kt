package tech.kaffa.portrait.codegen

import io.github.classgraph.*
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.ClassFileLocator
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import tech.kaffa.portrait.Includes
import tech.kaffa.portrait.Includes.*
import tech.kaffa.portrait.ProxyTarget
import tech.kaffa.portrait.Reflective
import tech.kaffa.portrait.codegen.scanner.ClasspathCollector
import tech.kaffa.portrait.codegen.scanner.ProxyCollector
import tech.kaffa.portrait.codegen.scanner.ReflectiveCollector
import tech.kaffa.portrait.codegen.utils.ClassGraphLocator
import java.util.*

class ClasspathScanner(private val classpath: String) {
    private lateinit var scanResult: ScanResult
    private lateinit var locator: ClassFileLocator
    private lateinit var pool: TypePool

    data class Result(
        val proxyTargets: Set<String>,
        val reflectives: Set<String>,
        val locator: ClassFileLocator,
        val result: ScanResult
    ) : AutoCloseable {
        override fun close() {
            try {
                locator.close()
            } catch (e: Exception) {
                logger.debug("Failed to close ClassFileLocator: ${e.message}")
            }
            try {
                result.close()
            } catch (e: Exception) {
                logger.debug("Failed to close ScanResult: ${e.message}")
            }
        }
    }

    // Instances
    private lateinit var reflectives: ClasspathCollector
    private lateinit var proxies: ClasspathCollector

    fun scan(): Result {
        val classgraph = ClassGraph()
        if (classpath.isNotBlank()) {
            classgraph.overrideClasspath(classpath)
        }
        scanResult = classgraph.enableAllInfo().scan()
        locator = ClassFileLocator.Compound(
            ClassGraphLocator(scanResult),
            ClassFileLocator.ForClassLoader.ofSystemLoader()
        )
        pool = TypePool.Default.of(locator)
        reflectives = ReflectiveCollector(scanResult, pool)
        proxies = ProxyCollector(scanResult, pool)

        collectDirectAnnotations()
        collectIncludeAnnotations()

        return Result(
            proxyTargets = proxies.collectedNames,
            reflectives = reflectives.collectedNames,
            locator = locator,
            result = scanResult
        )
    }

    private fun collectDirectAnnotations() {
        // @Reflective
        for (ci in scanResult.getClassesWithAnnotation(Reflective::class.java.name)) {
            reflectives.addName(ci.name)
            val includes = extractIncludes(
                ci.getAnnotationInfo(Reflective::class.java.name)
                    ?.parameterValues
                    ?.get("including")
                    ?.value)
            applyIncludes(ci, includes, reflectives)
        }
        // @ProxyTarget
        for (ci in scanResult.getClassesWithAnnotation(ProxyTarget::class.java.name)) {
            proxies.addName(ci.name)
            val includes = extractIncludes(
                ci.getAnnotationInfo(ProxyTarget::class.java.name)
                    ?.parameterValues
                    ?.get("including")
                    ?.value
            )
            applyIncludes(ci, includes, proxies)
        }
    }

    private fun collectIncludeAnnotations() {
        // @Reflective.Include
        for (holder in scanResult.getClassesWithAnnotation(Reflective.Include::class.java.name)) {
            val anns = holder.getAnnotationInfoRepeatable(Reflective.Include::class.java.name)
            for (ann in anns) {
                val classesArray = ann?.parameterValues?.get("classes")?.value as? Array<*> ?: continue
                val includes = extractIncludes(ann.parameterValues?.get("including")?.value)
                for (classRef in classesArray) {
                    val className = classRef.toString().removeSuffix(".class")
                    val ci = scanResult.getClassInfo(className)
                    if (ci != null) {
                        reflectives.addName(ci.name)
                        applyIncludes(ci, includes, reflectives)
                    } else {
                        reflectives.addName(className)
                        applyIncludesForName(className, includes, reflectives)
                    }
                }
            }
        }
        // @ProxyTarget.Include
        for (holder in scanResult.getClassesWithAnnotation(ProxyTarget.Include::class.java.name)) {
            val anns = holder.getAnnotationInfoRepeatable(ProxyTarget.Include::class.java.name)
            for (ann in anns) {
                val classesArray = ann?.parameterValues?.get("classes")?.value as? Array<*> ?: continue
                val includes = extractIncludes(ann.parameterValues?.get("including")?.value)
                for (classRef in classesArray) {
                    val className = classRef.toString().removeSuffix(".class")
                    proxies.addName(className)
                    val ci = scanResult.getClassInfo(className)
                    if (ci != null) {
                        applyIncludes(ci, includes, proxies)
                    } else {
                        applyIncludesForName(className, includes, proxies)
                    }
                }
            }
        }
    }

    private fun applyIncludes(targetClass: ClassInfo, includes: EnumSet<Includes>, c: ClasspathCollector) {
        if (includes.isEmpty()) return
        val targetType = typeOf(targetClass.name)

        if (DIRECT_SUBTYPES in includes) c.addDirectSubtypes(targetClass)
        if (ALL_SUBTYPES in includes) c.addAllSubtypes(targetClass)

        if (DIRECT_SUPERTYPES in includes) c.addDirectSupertypes(targetType)
        if (ALL_SUPERTYPES in includes) {
            c.addAllSupertypes(targetClass)
            c.addSupertypesViaTypePool(targetClass.name)
        }

        handlePublicApiIncludes(targetClass, targetType, includes, c)
    }

    private fun applyIncludesForName(className: String, includes: EnumSet<Includes>, c: ClasspathCollector) {
        if (includes.isEmpty()) return
        val targetType = typeOf(className)

        if (DIRECT_SUPERTYPES in includes) c.addDirectSupertypes(targetType)
        if (ALL_SUPERTYPES in includes) c.addSupertypesViaTypePool(className)

        handlePublicApiIncludes(null, targetType, includes, c)
    }

    // ---- Public API discovery (shared) --------------------------------------

    private fun handlePublicApiIncludes(
        targetClass: ClassInfo?,
        targetType: TypeDescription?,
        includes: EnumSet<Includes>,
        c: ClasspathCollector
    ) {
        if (!includes.includesPublicApi()) return

        val infos = mutableSetOf<ClassInfo>()
        val names = mutableSetOf<String>()
        collectPublicApiTypes(targetClass, targetType, infos, names)

        for (it in infos) {
            c.addName(it.name)
        }
        for (it in names) {
            c.addName(it)
        }

        if (PUBLIC_API_SUPERTYPES in includes) {
            for (it in infos) {
                c.addAllSupertypes(it)
            }
            for (it in names) {
                c.addSupertypesViaTypePool(it)
            }
        }
        if (PUBLIC_API_SUBTYPES in includes) {
            for (it in infos) {
                c.addAllSubtypes(it)
            }
        }
    }

    private fun EnumSet<Includes>.includesPublicApi(): Boolean =
        contains(PUBLIC_API) || contains(PUBLIC_API_SUBTYPES) || contains(PUBLIC_API_SUPERTYPES)

    private fun typeOf(name: String): TypeDescription? =
        pool.describe(name).takeIf { it.isResolved }?.resolve()

    private fun collectPublicApiTypes(
        targetClass: ClassInfo?,
        targetType: TypeDescription?,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>
    ) {
        targetClass?.let { collectPublicApiTypesFromClassInfo(it, collectedInfos, collectedNames) }
        val effective = targetType ?: targetClass?.name?.let { typeOf(it) }
        effective?.let { collectPublicApiTypesFromType(it, collectedInfos, collectedNames) }
    }

    private fun collectPublicApiTypesFromClassInfo(
        classInfo: ClassInfo,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>
    ) {
        for (f in classInfo.fieldInfo) {
            if (!f.isPublic) continue
            collectTypeClassInfos(f.typeSignatureOrTypeDescriptor, collectedInfos, collectedNames)
        }
        for (m in classInfo.methodInfo) {
            if (!m.isPublic) continue
            val sig = m.typeSignatureOrTypeDescriptor ?: continue
            if (!m.isConstructor) {
                collectTypeClassInfos(sig.resultType, collectedInfos, collectedNames)
            }
            for (p in m.parameterInfo) {
                collectTypeClassInfos(p.typeSignatureOrTypeDescriptor, collectedInfos, collectedNames)
            }
            for (thr in sig.throwsSignatures) {
                collectTypeClassInfos(thr, collectedInfos, collectedNames)
            }
        }
    }

    private fun collectPublicApiTypesFromType(
        type: TypeDescription,
        collectedInfos: MutableSet<ClassInfo>,
        collectedNames: MutableSet<String>,
        visited: MutableSet<String> = mutableSetOf()
    ) {
        for (f in type.declaredFields) {
            if (!f.isPublic) continue
            collectFromGeneric(f.type, collectedInfos, collectedNames, visited)
        }
        for (m in type.declaredMethods) {
            if (!m.isPublic) continue
            if (!m.isConstructor) {
                collectFromGeneric(m.returnType, collectedInfos, collectedNames, visited)
            }
            for (p in m.parameters) {
                collectFromGeneric(p.type, collectedInfos, collectedNames, visited)
            }
            for (ex in m.exceptionTypes) {
                collectFromGeneric(ex, collectedInfos, collectedNames, visited)
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

        val normalized = erasure.typeName
        if (!visited.add(normalized)) return

        scanResult.getClassInfo(normalized)?.let { collectedInfos.add(it) } ?: collectedNames.add(normalized)

        runCatching { generic.typeArguments }.getOrNull()?.forEach {
            collectFromGeneric(it, collectedInfos, collectedNames, visited)
        }
        runCatching { generic.upperBounds }.getOrNull()?.forEach {
            collectFromGeneric(it, collectedInfos, collectedNames, visited)
        }
        runCatching { generic.lowerBounds }.getOrNull()?.forEach {
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
                val className = signature.fullyQualifiedClassName
                val ci = signature.classInfo ?: scanResult.getClassInfo(className)
                if (ci != null) collectedInfos.add(ci) else collectedNames.add(className)
                signature.typeArguments?.forEach { collectTypeClassInfos(it, collectedInfos, collectedNames) }
                signature.suffixTypeArguments?.forEach { args ->
                    args.forEach {
                        collectTypeClassInfos(
                            it,
                            collectedInfos,
                            collectedNames
                        )
                    }
                }
            }

            is ArrayTypeSignature -> collectTypeClassInfos(
                signature.elementTypeSignature,
                collectedInfos,
                collectedNames
            )

            is TypeArgument -> collectTypeClassInfos(signature.typeSignature, collectedInfos, collectedNames)
            is TypeVariableSignature -> {
                signature.resolve()?.let { tp ->
                    tp.classBound?.let { collectTypeClassInfos(it, collectedInfos, collectedNames) }
                    tp.interfaceBounds?.forEach { collectTypeClassInfos(it, collectedInfos, collectedNames) }
                }
            }

            is MethodTypeSignature -> {
                collectTypeClassInfos(signature.resultType, collectedInfos, collectedNames)
                signature.throwsSignatures?.forEach { collectTypeClassInfos(it, collectedInfos, collectedNames) }
            }
        }
    }

    private fun extractIncludes(raw: Any?): EnumSet<Includes> {
        val includes = EnumSet.noneOf(Includes::class.java)
        val values = raw as? Array<*> ?: return includes
        for (value in values) {
            val name = value?.toString()?.substringAfterLast('.') ?: continue
            runCatching { Includes.valueOf(name) }.getOrNull()?.let { includes.add(it) }
        }
        return includes
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClasspathScanner::class.java)
    }
}
