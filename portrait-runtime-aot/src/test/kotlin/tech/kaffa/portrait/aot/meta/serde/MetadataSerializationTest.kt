package tech.kaffa.portrait.aot.meta.serde

import kotlin.test.Test
import tech.kaffa.portrait.aot.meta.PAnnotationEntry
import tech.kaffa.portrait.aot.meta.PClassEntry
import tech.kaffa.portrait.aot.meta.PConstructorEntry
import tech.kaffa.portrait.aot.meta.PFieldEntry
import tech.kaffa.portrait.aot.meta.PMethodEntry
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MetadataSerializationTest {

    @Test
    fun `MetadataSerializer and Deserializer roundtrip for PClassEntry`() {
        val serializer = MetadataSerializer()
        val deserializer = MetadataDeserializer()

        val proxyMethodEntry = PMethodEntry(
            name = "proxyMethod",
            parameterTypeNames = listOf("java.lang.String"),
            returnTypeName = "java.lang.Object",
            declaringClassName = "com.example.TestClass\$Proxy",
            isPublic = true,
            isPrivate = false,
            isProtected = false,
            isStatic = false,
            isFinal = false,
            isAbstract = false,
            annotations = emptyList(),
            parameterAnnotations = emptyList()
        )

        val originalEntry = PClassEntry(
            simpleName = "TestClass",
            qualifiedName = "com.example.TestClass",
            isAbstract = false,
            isSealed = false,
            isData = true,
            isCompanion = false,
            isObject = false,
            javaClassName = "com.example.TestClass",
            superclassName = "java.lang.Object",
            interfaceNames = listOf("java.io.Serializable"),
            annotations = emptyList(),
            constructors = emptyList(),
            declaredMethods = emptyList(),
            declaredFields = emptyList(),
            proxyMethods = listOf(proxyMethodEntry)
        )

        val serialized = serializer.serialize(originalEntry)
        val deserialized = deserializer.deserialize(serialized)

        assertEquals(originalEntry.simpleName, deserialized.simpleName)
        assertEquals(originalEntry.qualifiedName, deserialized.qualifiedName)
        assertEquals(originalEntry.isAbstract, deserialized.isAbstract)
        assertEquals(originalEntry.isSealed, deserialized.isSealed)
        assertEquals(originalEntry.isData, deserialized.isData)
        assertEquals(originalEntry.isCompanion, deserialized.isCompanion)
        assertEquals(originalEntry.isObject, deserialized.isObject)
        assertEquals(originalEntry.javaClassName, deserialized.javaClassName)
        assertEquals(originalEntry.superclassName, deserialized.superclassName)
        assertEquals(originalEntry.interfaceNames, deserialized.interfaceNames)
        assertEquals(originalEntry.proxyMethods, deserialized.proxyMethods)
    }

    @Test
    fun `MetadataSerializer handles PMethodEntry`() {
        val serializer = MetadataSerializer()
        val deserializer = MetadataDeserializer()

        val methodEntry = PMethodEntry(
            name = "testMethod",
            parameterTypeNames = listOf("java.lang.String", "int"),
            returnTypeName = "java.lang.Object",
            declaringClassName = "com.example.TestClass",
            isPublic = true,
            isPrivate = false,
            isProtected = false,
            isStatic = false,
            isFinal = true,
            isAbstract = false,
            annotations = emptyList(),
            parameterAnnotations = emptyList()
        )

        val classEntry = PClassEntry(
            simpleName = "TestClass",
            qualifiedName = "com.example.TestClass",
            isAbstract = false,
            isSealed = false,
            isData = false,
            isCompanion = false,
            isObject = false,
            javaClassName = "com.example.TestClass",
            superclassName = null,
            interfaceNames = emptyList(),
            annotations = emptyList(),
            constructors = emptyList(),
            declaredMethods = listOf(methodEntry),
            declaredFields = emptyList(),
            proxyMethods = emptyList()
        )

        val serialized = serializer.serialize(classEntry)
        val deserialized = deserializer.deserialize(serialized)

        assertEquals(1, deserialized.declaredMethods.size)
        val deserializedMethod = deserialized.declaredMethods[0]

        assertEquals(methodEntry.name, deserializedMethod.name)
        assertEquals(methodEntry.parameterTypeNames, deserializedMethod.parameterTypeNames)
        assertEquals(methodEntry.returnTypeName, deserializedMethod.returnTypeName)
        assertEquals(methodEntry.declaringClassName, deserializedMethod.declaringClassName)
        assertEquals(methodEntry.isPublic, deserializedMethod.isPublic)
        assertEquals(methodEntry.isStatic, deserializedMethod.isStatic)
        assertEquals(methodEntry.isFinal, deserializedMethod.isFinal)
    }

    @Test
    fun `MetadataSerializer handles PFieldEntry`() {
        val serializer = MetadataSerializer()
        val deserializer = MetadataDeserializer()

        val fieldEntry = PFieldEntry(
            name = "testField",
            typeName = "java.lang.String",
            declaringClassName = "com.example.TestClass",
            isPublic = false,
            isPrivate = true,
            isProtected = false,
            isStatic = false,
            isFinal = true,
            annotations = emptyList()
        )

        val classEntry = PClassEntry(
            simpleName = "TestClass",
            qualifiedName = "com.example.TestClass",
            isAbstract = false,
            isSealed = false,
            isData = false,
            isCompanion = false,
            isObject = false,
            javaClassName = "com.example.TestClass",
            superclassName = null,
            interfaceNames = emptyList(),
            annotations = emptyList(),
            constructors = emptyList(),
            declaredMethods = emptyList(),
            declaredFields = listOf(fieldEntry),
            proxyMethods = emptyList()
        )

        val serialized = serializer.serialize(classEntry)
        val deserialized = deserializer.deserialize(serialized)

        assertEquals(1, deserialized.declaredFields.size)
        val deserializedField = deserialized.declaredFields[0]

        assertEquals(fieldEntry.name, deserializedField.name)
        assertEquals(fieldEntry.typeName, deserializedField.typeName)
        assertEquals(fieldEntry.declaringClassName, deserializedField.declaringClassName)
        assertEquals(fieldEntry.isPublic, deserializedField.isPublic)
        assertEquals(fieldEntry.isPrivate, deserializedField.isPrivate)
        assertEquals(fieldEntry.isStatic, deserializedField.isStatic)
        assertEquals(fieldEntry.isFinal, deserializedField.isFinal)
    }

    @Test
    fun `MetadataSerializer handles PConstructorEntry`() {
        val serializer = MetadataSerializer()
        val deserializer = MetadataDeserializer()

        val constructorEntry = PConstructorEntry(
            declaringClassName = "com.example.TestClass",
            parameterTypeNames = listOf("java.lang.String", "int"),
            annotations = emptyList(),
            isPublic = true,
            isPrivate = false,
            isProtected = false
        )

        val classEntry = PClassEntry(
            simpleName = "TestClass",
            qualifiedName = "com.example.TestClass",
            isAbstract = false,
            isSealed = false,
            isData = false,
            isCompanion = false,
            isObject = false,
            javaClassName = "com.example.TestClass",
            superclassName = null,
            interfaceNames = emptyList(),
            annotations = emptyList(),
            constructors = listOf(constructorEntry),
            declaredMethods = emptyList(),
            declaredFields = emptyList(),
            proxyMethods = emptyList()
        )

        val serialized = serializer.serialize(classEntry)
        val deserialized = deserializer.deserialize(serialized)

        assertEquals(1, deserialized.constructors.size)
        val deserializedConstructor = deserialized.constructors[0]

        assertEquals(constructorEntry.declaringClassName, deserializedConstructor.declaringClassName)
        assertEquals(constructorEntry.parameterTypeNames, deserializedConstructor.parameterTypeNames)
        assertEquals(constructorEntry.isPublic, deserializedConstructor.isPublic)
        assertEquals(constructorEntry.isPrivate, deserializedConstructor.isPrivate)
        assertEquals(constructorEntry.isProtected, deserializedConstructor.isProtected)
    }

    @Test
    fun `MetadataSerializer handles PAnnotationEntry`() {
        val serializer = MetadataSerializer()
        val deserializer = MetadataDeserializer()

        val annotationEntry = PAnnotationEntry(
            annotationClassName = "com.example.TestAnnotation",
            simpleName = "TestAnnotation",
            qualifiedName = "com.example.TestAnnotation",
            properties = mapOf(
                "value" to "test",
                "number" to 42,
                "flag" to true
            )
        )

        val classEntry = PClassEntry(
            simpleName = "TestClass",
            qualifiedName = "com.example.TestClass",
            isAbstract = false,
            isSealed = false,
            isData = false,
            isCompanion = false,
            isObject = false,
            javaClassName = "com.example.TestClass",
            superclassName = null,
            interfaceNames = emptyList(),
            annotations = listOf(annotationEntry),
            constructors = emptyList(),
            declaredMethods = emptyList(),
            declaredFields = emptyList(),
            proxyMethods = emptyList()
        )

        val serialized = serializer.serialize(classEntry)
        val deserialized = deserializer.deserialize(serialized)

        assertEquals(1, deserialized.annotations.size)
        val deserializedAnnotation = deserialized.annotations[0]

        assertEquals(annotationEntry.annotationClassName, deserializedAnnotation.annotationClassName)
        assertEquals(annotationEntry.simpleName, deserializedAnnotation.simpleName)
        assertEquals(annotationEntry.qualifiedName, deserializedAnnotation.qualifiedName)
        assertEquals(annotationEntry.properties, deserializedAnnotation.properties)
    }

    @Test
    fun `MetadataSerializer handles complex class with all components`() {
        val serializer = MetadataSerializer()
        val deserializer = MetadataDeserializer()

        val complexClassEntry = PClassEntry(
            simpleName = "ComplexClass",
            qualifiedName = "com.example.ComplexClass",
            isAbstract = false,
            isSealed = true,
            isData = false,
            isCompanion = false,
            isObject = true,
            javaClassName = "com.example.ComplexClass",
            superclassName = "java.lang.Object",
            interfaceNames = listOf("java.io.Serializable", "java.lang.Comparable"),
            annotations = listOf(
                PAnnotationEntry(
                    annotationClassName = "com.example.TestAnnotation",
                    simpleName = "TestAnnotation",
                    qualifiedName = "com.example.TestAnnotation",
                    properties = mapOf("value" to "complex")
                )
            ),
            constructors = listOf(
                PConstructorEntry(
                    declaringClassName = "com.example.ComplexClass",
                    parameterTypeNames = emptyList(),
                    annotations = emptyList(),
                    isPublic = true,
                    isPrivate = false,
                    isProtected = false
                )
            ),
            declaredMethods = listOf(
                PMethodEntry(
                    name = "complexMethod",
                    parameterTypeNames = listOf("java.lang.String"),
                    returnTypeName = "int",
                    declaringClassName = "com.example.ComplexClass",
                    isPublic = true,
                    isPrivate = false,
                    isProtected = false,
                    isStatic = false,
                    isFinal = false,
                    isAbstract = false,
                    annotations = emptyList(),
                    parameterAnnotations = emptyList()
                )
            ),
            declaredFields = listOf(
                PFieldEntry(
                    name = "complexField",
                    typeName = "java.lang.String",
                    declaringClassName = "com.example.ComplexClass",
                    isPublic = false,
                    isPrivate = true,
                    isProtected = false,
                    isStatic = false,
                    isFinal = true,
                    annotations = emptyList()
                )
            ),
            proxyMethods = emptyList()
        )

        val serialized = serializer.serialize(complexClassEntry)
        assertNotNull(serialized)

        val deserialized = deserializer.deserialize(serialized)

        // Verify all properties are preserved
        assertEquals(complexClassEntry.simpleName, deserialized.simpleName)
        assertEquals(complexClassEntry.isSealed, deserialized.isSealed)
        assertEquals(complexClassEntry.isObject, deserialized.isObject)
        assertEquals(complexClassEntry.interfaceNames.size, deserialized.interfaceNames.size)
        assertEquals(complexClassEntry.annotations.size, deserialized.annotations.size)
        assertEquals(complexClassEntry.constructors.size, deserialized.constructors.size)
        assertEquals(complexClassEntry.declaredMethods.size, deserialized.declaredMethods.size)
        assertEquals(complexClassEntry.declaredFields.size, deserialized.declaredFields.size)
    }
}

