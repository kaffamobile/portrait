package tech.kaffa.portrait.codegen.scanner

import io.github.classgraph.ClassInfo
import io.github.classgraph.ScanResult
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.pool.TypePool

class ProxyCollector(scanResult: ScanResult, pool: TypePool) : ClasspathCollector(scanResult, pool) {
    override fun addName(name: String) {
        // Only collect interfaces as proxy targets
        val ci = getClassInfo(name)
        if (ci?.isInterface == true || (ci == null && isInterface(name))) {
            super.addName(name)
        }
    }

    override fun addDirectSubtypes(ci: ClassInfo) {
        for (it in ci.subclasses) {
            if (it.isInterface) addName(it.name)
        }
    }

    override fun addAllSubtypes(ci: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()
        fun dfs(current: ClassInfo) {
            if (!visited.add(current)) return
            current.subclasses.filter { it.isInterface }.forEach { addName(it.name); dfs(it) }
        }
        dfs(ci)
    }

    override fun addDirectSupertypes(td: TypeDescription?) {
        if (td == null) return
        for (iface in td.interfaces) {
            val n = normalizeTypeName(iface.asErasure().typeName) ?: continue
            getClassInfo(n)?.let { addName(it.name) } ?: run {
                // Only interfaces are meaningful as proxy targets; still track extras via base
                if (isInterface(n)) {
                    super.addName(n)
                }
            }
        }
    }

    override fun addAllSupertypes(ci: ClassInfo) {
        val visited = mutableSetOf<ClassInfo>()
        fun dfs(current: ClassInfo) {
            if (!visited.add(current)) return
            for (iface in current.interfaces) {
                addName(iface.name)
                dfs(iface)
            }
        }
        dfs(ci)
    }

    override fun addSupertypesViaTypePool(className: String): Set<String> =
        walkSupertypesViaTypePool(className, followSuperclass = false)
}