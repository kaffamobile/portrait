package tech.kaffa.portrait.aot;

import tech.kaffa.portrait.proxy.ProxyCreationException;
import tech.kaffa.portrait.proxy.ProxyHandler;

/**
 * Base class for static portrait reflection metadata.
 * DO NOT IMPLEMENT MANUALLY.
 * This class is meant for Portrait Codegen only.
 */
public abstract class StaticPortrait<T> {

    public abstract String getClassName();

    // Metadata
    public abstract String getMetadata();

    public T getObjectInstance() {
        throw new UnsupportedOperationException("Object instance not available");
    }

    public T[] getEnumConstants() {
        throw new UnsupportedOperationException("Class is not an enum.");
    }

    // Instance creation
    public T createProxy(ProxyMethodIndexer indexer, ProxyHandler<T> handler) {
        throw new ProxyCreationException("Proxy creation not supported");
    }

    public T invokeConstructor(int index, Object[] args) {
        throw new IndexOutOfBoundsException("No constructors available");
    }

    // Methods
    public Object invokeMethod(int index, Object instance, Object[] args) {
        throw new IndexOutOfBoundsException("No methods available");
    }

    // Fields
    public Object getFieldValue(int index, Object instance) {
        throw new IndexOutOfBoundsException("No fields available");
    }

    public void setFieldValue(int index, Object instance, Object value) {
        throw new IndexOutOfBoundsException("No fields available");
    }
}