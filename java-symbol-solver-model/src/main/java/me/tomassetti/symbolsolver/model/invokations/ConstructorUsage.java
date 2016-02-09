package me.tomassetti.symbolsolver.model.invokations;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.tomassetti.symbolsolver.model.declarations.ConstructorDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;

/**
 * @author Federico Tomassetti
 */
public class ConstructorUsage {
    private ConstructorDeclaration declaration;
    private List<TypeUsage> paramTypes = new ArrayList<>();

    public ConstructorUsage(ConstructorDeclaration declaration, TypeSolver typeSolver) {
        this.declaration = declaration;
        for (int i = 0; i < declaration.getNoParams(); i++) {
            paramTypes.add(declaration.getParam(i).getType());
        }
    }

    public ConstructorUsage(ConstructorDeclaration declaration, List<TypeUsage> paramTypes) {
        this.declaration = declaration;
        this.paramTypes = paramTypes;
    }

    private static TypeUsage replaceNameParam(String name, TypeUsage newValue, TypeUsage typeToBeExamined) {
        return typeToBeExamined.replaceParam(name, newValue);
    }

    @Override
    public String toString() {
        return "MethodUsage{" +
                "declaration=" + declaration +
                ", paramTypes=" + paramTypes +
                '}';
    }

    public ConstructorDeclaration getDeclaration() {
        return declaration;
    }

    public String getName() {
        return declaration.getName();
    }

    public TypeDeclaration declaringType() {
        return declaration.declaringType();
    }

    public List<TypeUsage> getParamTypes() {
        return paramTypes;
    }

    public ConstructorUsage replaceParamType(int i, TypeUsage replaced) {
        if (paramTypes.get(i) == replaced) {
            return this;
        }
        List<TypeUsage> newParams = new LinkedList<>(paramTypes);
        newParams.set(i, replaced);
        return new ConstructorUsage(declaration, newParams);
    }

    public int getNoParams() {
        return paramTypes.size();
    }

    public TypeUsage getParamType(int i, TypeSolver typeSolver) {
        return paramTypes.get(i);
    }

    public ConstructorUsage replaceNameParam(String name, TypeUsage typeUsage) {
        if (typeUsage == null) {
            throw new IllegalArgumentException();
        }
        // TODO if the method declaration has a type param with that name ignore this call
        ConstructorUsage res = this;
        for (int i = 0; i < paramTypes.size(); i++) {
            TypeUsage originalParamType = paramTypes.get(i);
            TypeUsage newParamType = originalParamType.replaceParam(name, typeUsage);
            res = res.replaceParamType(i, newParamType);
        }
        return res;
    }

}
