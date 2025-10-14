package tech.kaffa.portrait.codegen.scanner

import io.github.classgraph.ClassInfo
import io.github.classgraph.ScanResult
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.pool.TypePool

class ReflectiveCollector(scanResult: ScanResult, pool: TypePool) : ClasspathCollector(scanResult, pool) {
    override fun addDirectSubtypes(ci: ClassInfo) {
        ci.subclasses.forEach { addName(it.name) }
        if (ci.isInterface) ci.classesImplementing.forEach { addName(it.name) }
    }

    override fun addAllSubtypes(ci: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()
        fun dfs(current: ClassInfo) {
            if (!visited.add(current)) return
            current.subclasses.forEach { addName(it.name); dfs(it) }
            if (current.isInterface) {
                current.classesImplementing.forEach { addName(it.name); dfs(it) }
            }
        }
        dfs(ci)
    }

    override fun addDirectSupertypes(td: TypeDescription?) {
        if (td == null) return
        td.superClass?.let {
            val n = normalizeTypeName(it.asErasure().typeName) ?: return@let
            getClassInfo(n)?.let { addName(it.name) } ?: run {
                addName(n)
            }
        }
        for (it in td.interfaces) {
            val n = normalizeTypeName(it.asErasure().typeName) ?: continue
            getClassInfo(n)?.let { addName(it.name) } ?: run {
                addName(n)
            }
        }
    }

    override fun addAllSupertypes(ci: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()
        fun dfs(current: ClassInfo) {
            if (!visited.add(current)) return
            current.superclass?.let {
                addName(it.name)
                dfs(it)
            }
            for (it in current.interfaces) {
                addName(it.name)
                dfs(it)
            }
        }
        dfs(ci)
    }

    override fun addSupertypesViaTypePool(className: String): Set<String> =
        walkSupertypesViaTypePool(className, followSuperclass = true)
}