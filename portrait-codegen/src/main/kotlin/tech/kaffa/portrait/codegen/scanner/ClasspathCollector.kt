package tech.kaffa.portrait.codegen.scanner

import io.github.classgraph.ClassInfo
import io.github.classgraph.ScanResult
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.pool.TypePool
import org.slf4j.LoggerFactory
import java.util.LinkedHashSet

abstract class ClasspathCollector(
    private val scanResult: ScanResult,
    private val pool: TypePool
) {
    val collectedNames = linkedSetOf<String>()

    /** Add a discovered type name to this collector's set. */
    open fun addName(name: String) {
        collectedNames.add(name)
    }

    /** Add direct subtypes according to collector semantics. */
    abstract fun addDirectSubtypes(ci: ClassInfo)

    /** Add all subtypes according to collector semantics. */
    abstract fun addAllSubtypes(ci: ClassInfo)

    /** Add direct supertypes (e.g., superclass + interfaces, or only interfaces). */
    abstract fun addDirectSupertypes(td: TypeDescription?)

    /** Add all supertypes using ClassGraph graph traversal. */
    abstract fun addAllSupertypes(ci: ClassInfo)

    /** Add supertypes via ByteBuddy TypePool fallback, returns the discovered names. */
    abstract fun addSupertypesViaTypePool(className: String): Set<String>

    // ---- Shared helpers for subclasses ----------------------------------

    private fun resolveType(className: String): TypeDescription? {
        val n = normalizeTypeName(className) ?: return null
        return runCatching { pool.describe(n).resolve() }.onFailure {
            logger.debug("Failed to resolve type '$n': ${it.message}")
        }.getOrNull()
    }

    protected fun getClassInfo(name: String): ClassInfo? = scanResult.getClassInfo(name)

    protected fun normalizeTypeName(typeName: String?): String? {
        val nn = typeName ?: return null
        if (nn.isBlank()) return null
        var normalized = nn
        while (normalized.endsWith("[]")) normalized = normalized.removeSuffix("[]")
        if (normalized in PRIMITIVE_NAMES || normalized.isEmpty()) return null
        return normalized
    }

    protected fun isInterface(name: String): Boolean =
        resolveType(name)?.isInterface == true

    /** Fallback walker helper: adds discovered supertypes via addName/addExtraName. */
    protected fun walkSupertypesViaTypePool(
        startName: String,
        followSuperclass: Boolean,
    ): Set<String> {
        val normalized = normalizeTypeName(startName) ?: return emptySet()
        val discovered = LinkedHashSet<String>()
        val visited = mutableSetOf<String>()

        fun walk(name: String) {
            if (!visited.add(name)) return
            val type = resolveType(name) ?: return

            if (followSuperclass) {
                type.superClass?.let {
                    val n = normalizeTypeName(it.asErasure().typeName)
                    if (n != null && n != name) {
                        discovered.add(n)
                        if (getClassInfo(n) == null) {
                            addName(n)
                        } else addName(n)
                        walk(n)
                    }
                }
            }
            for (it in type.interfaces) {
                val n = normalizeTypeName(it.asErasure().typeName)
                if (n != null && n != name) {
                    discovered.add(n)
                    if (getClassInfo(n) == null) {
                        addName(n)
                    } else addName(n)
                    walk(n)
                }
            }
        }

        walk(normalized)
        return discovered
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClasspathCollector::class.java)
        private val PRIMITIVE_NAMES = setOf(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"
        )
    }
}