# TeaVM Class Library Mapping & JVM Integration

## Why This Matters
TeaVM ships its own class library under `org.teavm.classlib.*`, but it impersonates JDK classes such as `java.lang.Object` when compiling to JavaScript or WebAssembly. Understanding how that aliasing works lets you reuse the same logic on the JVM (for example, with ByteBuddy) so you can read the TeaVM classlib while pretending it is the standard JDK.

## How TeaVM Rewrites Class Names
- **Classpath loader** – `ClasspathClassHolderSource` wraps TeaVM’s low-level class reader and plugs in a `RenamingResourceMapper` before any class is exposed to the compiler (`core/src/main/java/org/teavm/parsing/ClasspathClassHolderSource.java:21`).
- **Mapping rules** – `RenamingResourceMapper` loads every `META-INF/teavm.properties` that it can find on the classpath and records prefix rules, package redirects, and inclusions (`core/src/main/java/org/teavm/parsing/RenamingResourceMapper.java:39-231`).
- **TeaVM classlib rules** – the bundled properties file rewrites `org.teavm.classlib.java.*` into `java.*` and strips the leading `T` from class names (`classlib/src/main/resources/META-INF/teavm.properties:14-18`).
- **Prefix handling** – `PrefixMapping` applies and removes the leading `T` whenever a class lookup crosses package boundaries (`core/src/main/java/org/teavm/parsing/substitution/PrefixMapping.java:1-61`).
- **Reference rewriting** – after the `.class` file is read, `ClassRefsRenamer` walks every constant-pool reference so the resulting `ClassHolder` looks as if it always lived in `java.*`.

### Example Flow: Resolving `java.lang.Object`
1. TeaVM asks the loader for `java.lang.Object`.
2. `RenamingResourceMapper` consults the prefix rules and translates the request to `org.teavm.classlib.java.lang.TObject`.
3. The bytecode is read from `classlib/src/main/java/org/teavm/classlib/java/lang/TObject.java`’s compiled output.
4. `ClassRefsRenamer` rewrites the class name and every type reference to `java.lang.*`.
5. The compiler receives a `ClassHolder` whose public name is `java.lang.Object`, even though it came from `TObject`.

The same rules cover the rest of the TeaVM classlib (for example, `TString` → `java.lang.String`, `TThrowable` → `java.lang.Throwable`, and so on).

## JVM/ByteBuddy Strategy
To reproduce this behaviour inside a JVM agent or tool, mimic TeaVM’s loader when ByteBuddy requests class files.

### 1. Load the Mapping Rules
```java
var referenceCache = new ReferenceCache();
var resourceProvider = new ClasspathResourceProvider(classLoader);
var mapper = new RenamingResourceMapper(resourceProvider, referenceCache, name -> {
    try (var resource = resourceProvider.getResource(name.replace('.', '/') + ".class")) {
        return resource != null ? ResourceClassHolderMapper.readClass(resource.open(), referenceCache) : null;
    }
});
```
You can depend on TeaVM’s parsing classes directly, or port the small helper types if you need a standalone solution.

### 2. Implement a ClassFileLocator
```java
public final class TeaVmClassFileLocator implements ClassFileLocator {
    private final ClassLoader loader;
    private final TeaVmNameMapper mapper; // thin wrapper around RenamingResourceMapper logic

    @Override
    public Resolution locate(String typeName) throws IOException {
        String mapped = mapper.toTeaVmInternal(typeName); // java.lang.Object -> org/teavm/classlib/java/lang/TObject
        try (InputStream in = loader.getResourceAsStream(mapped + ".class")) {
            if (in == null) {
                return new Resolution.Illegal(typeName);
            }
            byte[] bytes = in.readAllBytes();
            byte[] remapped = mapper.rewriteConstantPool(bytes); // undo the T-prefix & package renames
            return new Resolution.Explicit(remapped);
        }
    }
}
```
- `toTeaVmInternal` converts from the public binary name to TeaVM’s internal name using the same prefix rules.
- `rewriteConstantPool` can delegate to TeaVM’s `ClassRefsRenamer` or an ASM `ClassRemapper` that undoes the TeaVM-to-JDK renaming.

### 3. Plug Into ByteBuddy
```java
ClassFileLocator locator = new TeaVmClassFileLocator(classLoader, mapper);
TypePool typePool = new TypePool.Default.WithLazyResolution(TypePool.CacheProvider.Simple.of(), locator);

new ByteBuddy()
        .with(typePool)
        .redefine(Object.class, locator)
        .make();
```
Use the custom locator anywhere ByteBuddy needs to resolve classes. The remapped bytecode will show `java/lang/Object` in its constant pool, letting ByteBuddy treat TeaVM classes as if they were the real JDK types.

## Implementation Checklist
- [ ] Load `META-INF/teavm.properties` and build the same package/prefix tables TeaVM uses.
- [ ] Provide a binary-name → TeaVM-name mapper.
- [ ] Wrap classfile resolution in a `ClassFileLocator`.
- [ ] Rewrite returned bytecode back to the public names before handing it to ByteBuddy.
- [ ] Add regression tests (for example, assert that locating `java.lang.Object` yields a class whose internal name is `java/lang/Object` and contains methods like `wait`/`notify`).

## Further Reading
- TeaVM’s comparison tool relies on the same mechanism: `tools/classlib-comparison-gen/.../JCLComparisonBuilder.java:121-210`.
- TeaVM class library sources live under `classlib/src/main/java/org/teavm/classlib/java/...`.
- For pure JVM usage you only need the parsing utilities; the rest of the compiler is optional.
