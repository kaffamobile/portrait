# Reflection Usage Report - Espresso3 Framework

This document catalogs all usages of Java Reflection, org.reflections library, and Kotlin Reflection within the
Espresso3 codebase, organized by reflection patterns and techniques.

## Summary

The Espresso3 framework uses reflection across four main categories:

- **Class-based reflection**: Dynamic class loading, type inspection, and instance creation
- **Method-based reflection**: Dynamic method invocation and discovery
- **Field-based reflection**: Property and field introspection
- **Scanning reflection**: Classpath scanning and type discovery

## Reflection Usage Patterns

### 1. Scanning Reflection

#### org.reflections Library Usage

**Primary Location**: `src/e3-core/src/main/kotlin/kaffa/e3/metamodel/kotlin/metamodel.kt`

The org.reflections library provides comprehensive classpath scanning capabilities:

**Imports and Setup**:

```kotlin
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ConfigurationBuilder
```

**Key Usage Areas**:

1. **TypedModule Discovery** (lines 881-962):
    - Scans classpath for TypedModule subclasses
    - Uses XML-based reflection metadata when available
    - Falls back to runtime scanning when XML not found
    - Caches reflection results for performance

2. **Metadata Class Discovery** (lines 925-937):
    - Finds all subclasses of `TypedRootMetadata`
    - Filters out companion objects and EntityTemplates
    - Builds reflection cache with soft values

**Critical Implementation Details**:

```kotlin
private val reflectionsCache = CacheBuilder.newBuilder().softValues().build<String, Reflections>()

@Synchronized
private fun buildReflections(pkgName: String, classLoader: ClassLoader? = null): Reflections {
    return reflectionsCache.get(pkgName) {
        val reflectionsXmlFullname = "/$REFLECTIONS_PACKAGE_PREFIX/$pkgName-reflections.xml"
        
        val resource = (classLoader?.getResourceAsStream(reflectionsXmlFullname) 
            ?: this::class.java.getResourceAsStream(reflectionsXmlFullname))?.use {
            it.readBytes()
        }?.inputStream()

        resource.use { reflectionsXmlStream ->
            if (reflectionsXmlStream !== null) {
                logger.info("Reflection XML was found for package ({})", pkgName)
                Reflections(ConfigurationBuilder()).apply {
                    collect(reflectionsXmlStream)
                }
            } else {
                logger.debug("Reflection XML was not found for package ({})", pkgName)
                val metamodelPkg = TypedRootMetadata::class.qualifiedName!!.substringBeforeLast('.')
                logger.debug("building reflections. MetamodelPkg: $metamodelPkg")

                val params = mutableListOf(pkgName, metamodelPkg, SubTypesScanner(), 
                    * additionalReflectionClasses.map { it.substringBeforeLast(".") }.toTypedArray())
                if (classLoader != null) {
                    params.add(classLoader)
                }
                val builder = ConfigurationBuilder.build(*params.toTypedArray())
                Reflections(builder)
            }
        }
    }
}
```

### 2. Class-based Reflection

#### Dynamic Class Loading (Class.forName)

**Core Module Registry** (`src/e3-core/src/main/kotlin/kaffa/e3/core/model/ModelRegistry.kt`):

```kotlin
// Lines 58, 108 - Dynamic factory class loading
val kclass = Class.forName(factoryClass).kotlin
```

**Plugin System** (`src/e3-plugin/src/main/kotlin/kaffa/e3/plugin/Plugin.kt`):

```kotlin
// Lines 74, 85 - Plugin class loading and caching
return cacheFile.readLines().map { Class.forName(it) }
autoServices.add(Class.forName(className))
```

**Integration Services** (
`src/e3-integration/src/main/kotlin/kaffa/e3/integration/services/IntegrationSourceFactory.kt`):

```kotlin
// Lines 172, 176 - Dynamic converter loading
Class.forName(converter) as Class<*>
Class.forName("kaffa.e3.integration.${converter}Factory") as Class<*>
```

**Database Drivers**:

- **DuckDB**: `src/e3-persistence-duckdb/src/main/kotlin/kaffa/e3/persistence/duckdb/persister/DuckDBPersister.kt:54`
- **Generic JDBC**: `src/e3-jdbcmanager/src/main/java/br/com/kaffa/jdbc/GenericDataSource.kt:17`

**Optional Components**:

- **Lua Scripting**: `src/e3-core/src/main/kotlin/kaffa/e3/model/utils/scripting/lua/LuaScripting.kt` (lines 12, 29, 51)
- **CSV Metadata**: `src/e3-core/src/main/kotlin/kaffa/e3/model/csv/CsvMetadataLoader.kt:31`

#### Type Checking and Introspection

**Map Utilities** (`src/app-base/e3-app-base/src/main/kotlin/kaffa/e3/appbase/ui/map/MapUtils.kt`):

```kotlin
// Line 237 - Runtime type checking
val isPersisterVersioned =
    SpatialiteVersionedDBManager::class.java.isAssignableFrom(DBManager.getDBManager(e3).javaClass)
```

#### Instance Creation

**GIS Module Loader** (`src/e3-gis/src/main/java/kaffa/e3\gis\model\E3GisModuleLoader.java`):

```java
// Lines 253 - Dynamic constructor invocation
final Constructor<?> c = addColumnMigrationClass.getDeclaredConstructor(String.class, String.class);
```

**Plugin System** (`src/e3-plugin/src/main/kotlin/kaffa/e3/plugin/Plugin.kt`):

```kotlin
// Line 64 - Dynamic instance creation
serviceClass.cast(clazz.newInstance())
```

### 3. Method-based Reflection

#### Dynamic Method Discovery and Invocation

**REST Client** (`src/e3-rest-client/src/main/java/kaffa/e3/rest/client/ClientRest.java`):

```java
// Lines 285, 305, 348 - Dynamic method invocation
for(Method m :Class.

forName(className).

getMethods()){
types[i]=Class.

forName(args[typesOffset+i]);
return Class.

forName(className).

getMethod(methodName, types);
```

**GraphQL Schema Builder** (`src/e3-graphql/src/main/kotlin/kaffa/e3/graphql/GraphQLSchemaBuilder.kt`):

```kotlin
// Lines 192, 236 - Method proxy for GraphQL resolvers
val originalMethod = originalQuerySet::class.java.getMethod(method.name, *method.parameterTypes)
val originalMethod = originalCmdHandler::class.java.getMethod(method.name, *method.parameterTypes)
```

**PostgreSQL Test Utilities** (
`src/e3-persistence-postgres/src/test/kotlin/kaffa/e3/persistence/postgres/persister/PostgresPersisterTest.kt`):

```kotlin
// Lines 33, 35 - Testing private methods
val result = persister::class.java.getDeclaredMethod("mapValuesMainToPersistenceModel", EntityData::class.java)
    .invoke(persister, entityData) as Map<String, Any?>
```

### 4. Field-based Reflection

#### Annotation Processing

**Plugin Props** (`src/e3-plugin/src/main/kotlin/kaffa/e3/plugin/Plugin.kt`):

```kotlin
// Lines 47, 128 - Plugin annotation inspection
it.javaClass.getAnnotation(PluginProps::class.java)?.primary == true
val required = serviceClass.getAnnotation(ExtensionPoint::class.java)?.required
```

**Module Reading** (`src/e3-core/src/main/kotlin/kaffa/e3/model/utils/ModuleReaderUtils.kt`):

```kotlin
// Lines 79, 80 - XML module options from annotations
val readerOptions = method.getAnnotation(XmlModuleOptions::class.java)
val readerOptionsClass = dataType.getAnnotation(XmlModuleOptions::class.java)
```

**Enkapsulation Info** (`src/e3-core/src/main/kotlin/kaffa/e3\model\kapsule\EnkapsulationInfo.kt`):

```kotlin
// Lines 95, 96 - Enkapsulation configuration
val optionsFromMethod: Enkapsulation? = method.getAnnotation(Enkapsulation::class.java)
val optionsFromTarget: Enkapsulation? = targetClass.getAnnotation(Enkapsulation::class.java)
```

### 5. Kotlin Reflection Usage

#### Object Instance Creation and Type Introspection

**Model Registry** (`src/e3-core/src/main/kotlin/kaffa/e3/core/model/ModelRegistry.kt`):

```kotlin
// Lines 59, 109 - Kotlin object instance or constructor-based creation
val obj = kclass.objectInstance ?: kclass.createInstance()
```

**XML Model Reader** (`src/e3-core/src/main/kotlin/kaffa/e3/model/xml/XmlModelReader.kt`):

```kotlin
// Line 526 - TypedModule object instance loading
val typedModuleObject = ClassLoaderUtils.loadClass(typedModuleAtt).kotlin.objectInstance as TypedModule
```

**Module Reader Utilities** (`src/e3-core/src/main/kotlin/kaffa/e3/model/utils/ModuleReaderUtils.kt`):

```kotlin
// Lines 473, 486 - Object instance fallback for classes without default constructors
c.kotlin.objectInstance
    ?: throw ModelException("Class $strValue doesn't have a default constructor and is not an object.")
c.kotlin.objectInstance
```

**Meta-model System** (`src/e3-core/src/main/kotlin/kaffa/e3/metamodel/kotlin/metamodel.kt`):

```kotlin
// Lines 930-935 - Comprehensive Kotlin reflection for metadata discovery
buildReflections(pkgName, classLoader).getSubTypesOf(TypedRootMetadata::class.java).map { clazz ->
    clazz.kotlin
}.filter {
    !it.isCompanion  // Filter out companion objects
}.mapNotNull { clazz ->
    clazz.objectInstance  // Get singleton instances
}.filter { it !is EntityTemplate }
```

#### Class Information and Names

**UI Components** (`src/app-base/e3-app-base-desktop/src/test/java/kiui/poc/components.kt`):

```kotlin
// Lines 555, 563, 568 - Location class reflection for UI routing
locationClass = location.javaClass.kotlin
text = it.javaClass.simpleName
Text(location.javaClass.simpleName)
```

**Persistence Tests** (
`src/e3-persistence/src/test/kotlin/kaffa/e3/persistence/spatialite/activityarchive/tests/TestActivityArchiveAspect.kt`):

```kotlin
// Lines 41-43 - Strategy class name extraction
val geomStrategy = GeomLocalBaseCopyStrategy.javaClass.kotlin.simpleName
val relatedStrategy = RelatedLocalBaseCopyStrategy.javaClass.kotlin.simpleName
val fullStrategy = FullLocalBaseCopyStrategy.javaClass.kotlin.simpleName
```

**Mapbox Integration** (`src/app-base/e3-app-base-desktop/src/main/kotlin/kaffa/mapbox`):

```kotlin
// Thread factory naming using class reflection
ThreadFactoryBuilder().setNameFormat("${ThreadSafeMapboxNativeBridge::class.simpleName}-%d").build()
logger.debug("*Initializing ${MapboxNativeBridge::class.simpleName}")
logger.debug("*Destroying ${MapboxNativeBridge::class.simpleName}")
```

#### Core Framework Integration

**Context System** (`src/e3-core/src/main/kotlin/kaffa/e3/core/kotlin/e3-core.kt`):

```kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod

// Context value delegation with reflection
operator fun <V : Any?> getValue(arg: V, property: KProperty<*>): T
operator fun setValue(contextBuilder: ContextBuilder, property: KProperty<*>, t: T)

// Type-safe context operations  
fun <T : Any> contextVal(klass: KClass<T>): ContextVal<T>
inline fun <T : Any> ContextBuilder.set(serviceKClass: KClass<T>, serviceImpl: T)
```

**CLI Argument Parsing** (`src/e3-cli/src/main/kotlin/kaffa/e3/cli/ArgsParser.kt`):

```kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

// Comprehensive reflection-based argument parsing
fun <T: Any> parseData(args: Array<String>, dataClass: KClass<T>): DataArgsResult<T>
private fun <T: Any> coerceValue(value: String?, kClass: KClass<T>): T?
```

#### Model and Metadata System

**XML Model Reading** (`src/e3-core/src/main/kotlin/kaffa/e3/model/xml/XmlModelReader.kt`):

```kotlin
import kotlin.reflect.KClass

// Data type to KClass conversion utilities
?.let { dataTypeToKClass(it) }
xmlReader.readElement((ConstReader(dataTypeToKClass(dataType))), eventReader, event)
private class ConstReader(klass: KClass<*>) : ElementReader<kaffa.e3.core.Pair<Any?, String>>
```

**Module Reader Utilities** (`src/e3-core/src/main/kotlin/kaffa/e3/model/utils/ModuleReaderUtils.kt`):

```kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

// Data class reflection for metadata processing
class DataClassReader(val aspectAlias: String, val metaAtt: String, val dataType: KClass<*>, attributes: Array<String>)
class DataClassXmlWriter(
    val dataType: KClass<*>,
    val metaAttAlias: String,
    attributes: Array<String>,
    val isCollection: Boolean
)

private val properties: List<KProperty1<*, Any?>>

// Utility functions for type conversion
fun dataTypeToKClass(dataType: String?): KClass<*>
fun kClassToDataType(kclass: KClass<*>): String
```

**Model Registry** (`src/e3-core/src/main/kotlin/kaffa/e3/core/model/ModelRegistry.kt`):

```kotlin
import kotlin.reflect.full.createInstance

// Dynamic factory instance creation
val kclass = Class.forName(factoryClass).kotlin
```

#### Configuration and Options

**PostgreSQL Options** (
`src/e3-persistence-postgres/src/main/kotlin/kaffa/e3/persistence/postgres/options/E3PostgresOptionsBuilder.kt`):

```kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

// Reflection-based configuration building
.mapNotNull { param -> param.name?.let { paramName -> paramName to props[paramName]?.invoke(template) } }
```

**DuckDB Options** (
`src/e3-persistence-duckdb/src/main/kotlin/kaffa/e3/persistence/duckdb/options/E3DuckDBOptionsBuilder.kt`):

```kotlin
// Similar pattern for DuckDB configuration
.mapNotNull { param -> param.name?.let { paramName -> paramName to props[paramName]?.invoke(template) } }
```

#### Meta-model System

**Core Metamodel** (`src/e3-core/src/main/kotlin/kaffa/e3/metamodel/kotlin/metamodel.kt`):

```kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

// TypedEntity attribute property building
fun buildAttributeProps(typedEntity: TypedEntity): List<KProperty1<TypedEntity, TypedAttribute<*>>> {
    val list = typedEntity.javaClass.kotlin.memberProperties.filter {
        it.javaGetter?.returnType == TypedAttribute::class.java
    }
    
    @Suppress("UNCHECKED_CAST")
    val props = list.filterNotNull() as List<KProperty1<TypedEntity, TypedAttribute<*>>>
    return props
}

// TypedModule metadata discovery
return getMetadataClasses(pkgName, this::class.java.classLoader).filter { obj ->
    obj.javaClass.`package` == pkg
}.onEach {
    it._module = this
}
```

## Advanced Patterns and Optimizations

### XML-based Reflection Metadata

The framework supports pre-compiled reflection metadata through XML files:

- **Location**: `META-INF/reflections/${package-name}-reflections.xml`
- **Purpose**: Avoid runtime classpath scanning in production
- **Fallback**: Runtime reflection when XML not available

### Plugin Extension Points

The framework uses `@ExtensionPoint` annotation with reflection-based discovery:

```kotlin
@ExtensionPoint
interface MetadataExtension {
    fun listRoots(): List<Class<out TypedRootMetadata>>
}
```

### Performance Optimizations

- **Soft reference caching** for reflection results
- **XML metadata pre-compilation** to avoid runtime scanning
- **Package-based reflection scope** limiting to reduce scan time

## Framework Architecture Analysis

### Core Components Using Reflection

1. **Plugin System** - Dynamic service discovery and loading
2. **Metamodel Architecture** - Model-driven development with type introspection
3. **Configuration Systems** - Dynamic configuration building and validation
4. **Integration Services** - Runtime converter and factory loading

### Performance Characteristics

1. **Startup Phase** - Heavy reflection usage during application initialization
2. **Runtime Phase** - Cached reflection results for optimal performance
3. **Memory Usage** - Soft reference caching to balance memory and performance

## Files with Significant Reflection Usage

| File                   | Category | Usage Type                      | Lines of Interest                                                                  |
|------------------------|----------|---------------------------------|------------------------------------------------------------------------------------|
| `metamodel.kt`         | Core     | org.reflections, Kotlin reflect | 60-62, 68-71, 488-497, 881-962                                                     |
| `Plugin.kt`            | Core     | Java reflection, Kotlin reflect | 11, 42-47, 64, 74, 85, 116-117                                                     |
| `ArgsParser.kt`        | CLI      | Kotlin reflect                  | 8-11, 138, 150, 215, 339, 357                                                      |
| `ModelRegistry.kt`     | Core     | Java reflection, Kotlin reflect | 13, 58, 108                                                                        |
| `ModuleReaderUtils.kt` | Core     | Kotlin reflect, annotations     | 28-33, 79-80, 124, 145, 183, 257, 274, 320, 411, 423                               |
| `e3-core.kt`           | Core     | Kotlin reflect                  | 36-39, 45, 70, 74, 78, 82, 90, 94, 103, 108, 112, 116, 124, 167, 171, 178-283, 355 |

## Reflection Usage Summary by Pattern

Based on the analysis, here are the reflection patterns categorized by type and frequency:

### Class-based Reflection (most common)

1. **`Class.forName()`** - 18+ occurrences across 12 files
    - Used for dynamic class loading in plugins, integration services, and database drivers
2. **`.kotlin`** - 50+ references across many files
    - Used to convert Java Class to Kotlin KClass for type-safe operations
3. **`.objectInstance`** - 6+ occurrences
    - Used for singleton object access in metamodel and configuration
4. **`.simpleName`** - 10+ occurrences
    - Used for logging, UI display, and debugging
5. **`.isAssignableFrom()`** - 1 occurrence
    - Used for runtime type checking

### Method-based Reflection

1. **`.getMethod()` / `.getDeclaredMethod()`** - 4+ occurrences
    - Used in GraphQL schema building and testing utilities
2. **`.invoke()`** - 3+ occurrences
    - Used for dynamic method invocation in REST clients and tests

### Field-based Reflection

1. **`.getAnnotation()`** - 6+ occurrences
    - Used for annotation-based configuration and plugin metadata
2. **`.memberProperties`** - 5+ occurrences
    - Used for property introspection in metamodel and configuration builders

### Instance Creation Reflection

1. **`.newInstance()`** - 3+ occurrences
    - Used for dynamic object instantiation in plugin system
2. **`.createInstance()`** - 3+ occurrences
    - Used as fallback when objectInstance is null

### Scanning Reflection

1. **`Reflections.getSubTypesOf()`** - Primary usage pattern
    - Used for scanning class hierarchies in metamodel system
2. **`ConfigurationBuilder.build()`** - Configuration pattern
    - Used to set up reflection scanning parameters
3. **XML-based reflection cache** - Performance optimization
    - Used to avoid runtime classpath scanning in production
4. **`.isCompanion`** - 1 occurrence
    - Used to filter out companion objects during reflection scanning

## Recommendations

1. **Documentation**: Add comprehensive documentation for reflection-based components
2. **Testing**: Ensure reflection-based code has adequate test coverage
3. **Performance**: Monitor startup times and consider lazy initialization where possible
4. **Security**: Implement proper validation for dynamically loaded classes
5. **Modernization**: Consider using newer Kotlin reflection APIs where applicable
6. **Caching**: Expand use of reflection result caching to improve performance
7. **Monitoring**: Add metrics for reflection-heavy operations to track performance impact