package me.tomassetti.symbolsolver.model.declarations;

import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsage;

/**
 * Declaration of an Enum.
 *
 * @author Federico Tomassetti
 */
public interface EnumDeclaration extends TypeDeclaration {

    @Override
    default boolean isEnum() {
        return true;
    }
    
    ReferenceTypeUsage getSuperClass();
}
