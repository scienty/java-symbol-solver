package me.tomassetti.symbolsolver.model.typesystem;

import me.tomassetti.symbolsolver.model.resolution.TypeParameter;

/**
 * A usage of a type. It could be a primitive type or a reference type (enum, class, interface).
 * In the later case it could take type parameters (other TypeUsages). It could also be a TypeVariable, like in:
 * <p/>
 * class A<B> { }
 * <p/>
 * where B is a TypeVariable. It could also be Wildcard Type, possibly with constraints.
 *
 * @author Federico Tomassetti
 */
public interface TypeUsage {

    ///
    /// Relation with other types
    ///

    /**
     * Does this type represent an array?
     */
    default boolean isArray() {
        return false;
    }

    /**
     * Is this a primitive type?
     */
    default boolean isPrimitive() {
        return false;
    }

    /**
     * Is this the null type?
     */
    default boolean isNull() {
        return false;
    }

    /**
     * Is this a non primitive value?
     */
    default boolean isReference() {
        return isReferenceType() || isArray() || isTypeVariable() || isNull() || isClassType();
    }

    /**
     * Can this be seen as a ReferenceTypeUsage?
     * In other words: is this a reference to a class, an interface or an enum?
     */
    default boolean isReferenceType() {
        return false;
    }

    default boolean isVoid() {
        return false;
    }

    default boolean isTypeVariable() {
        return false;
    }

    default boolean isWildcard() {
        return false;
    }
    
    default boolean isClassType() {
        return false;
    }

    ///
    /// Downcasting
    ///

    default ArrayTypeUsage asArrayTypeUsage() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName());
    }

    default ReferenceTypeUsage asReferenceTypeUsage() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName());
    }

    default TypeParameter asTypeParameter() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName());
    }

    default PrimitiveTypeUsage asPrimitive() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName());
    }

    default WildcardUsage asWildcard() {
        throw new UnsupportedOperationException(this.getClass().getCanonicalName());
    }

    ///
    /// Naming
    ///

    String describe();

    ///
    /// TypeParameters
    ///

    default TypeUsage replaceParam(String name, TypeUsage replaced) {
        return this;
    }

    ///
    /// Relation with other types
    ///

    boolean isAssignableBy(TypeUsage other);

}
