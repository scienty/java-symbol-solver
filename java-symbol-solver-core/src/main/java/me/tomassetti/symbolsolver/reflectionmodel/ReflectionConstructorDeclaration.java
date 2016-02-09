package me.tomassetti.symbolsolver.reflectionmodel;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ModifierSet;

import me.tomassetti.symbolsolver.model.declarations.ConstructorDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ParameterDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.invokations.ConstructorUsage;
import me.tomassetti.symbolsolver.model.invokations.MethodUsage;
import me.tomassetti.symbolsolver.model.resolution.Context;
import me.tomassetti.symbolsolver.model.resolution.TypeParameter;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;

public class ReflectionConstructorDeclaration implements ConstructorDeclaration {
	
	//TODO: typed
    private Constructor constructor;
    private TypeSolver typeSolver;

    public ReflectionConstructorDeclaration(Constructor constructor, TypeSolver typeSolver) {
        this.constructor = constructor;
        if (constructor.isSynthetic() ) {
            throw new IllegalArgumentException();
        }
        this.typeSolver = typeSolver;
    }

    @Override
    public String getName() {
        return constructor.getName();
    }

    @Override
    public boolean isField() {
        return false;
    }

    @Override
    public boolean isParameter() {
        return false;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public String toString() {
        return "ReflectionConstructorDeclaration{" +
                "constructor=" + constructor +
                '}';
    }

    @Override
    public boolean isType() {
        return false;
    }

    @Override
    public TypeDeclaration declaringType() {
        if (constructor.getDeclaringClass().isInterface()) {
            return new ReflectionInterfaceDeclaration(constructor.getDeclaringClass(), typeSolver);
        } else {
            return new ReflectionClassDeclaration(constructor.getDeclaringClass(), typeSolver);
        }
    }
    
    @Override
    public List<TypeUsage> getExceptionTypes() {
    	return Arrays.stream(constructor.getExceptionTypes()).map((exType) -> ReflectionFactory.typeUsageFor(exType, typeSolver)).collect(Collectors.toList());
    }

    @Override
    public int getNoParams() {
        return constructor.getParameterTypes().length;
    }

    @Override
    public ParameterDeclaration getParam(int i) {
        boolean variadic = false;
        if (constructor.isVarArgs()) {
            variadic = i == (constructor.getParameterCount() - 1);
        }
        return new ReflectionParameterDeclaration(constructor.getParameterTypes()[i], constructor.getGenericParameterTypes()[i], typeSolver, variadic);
    }

    public MethodUsage getUsage(Node node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TypeParameter> getTypeParameters() {
        return Arrays.stream(constructor.getTypeParameters()).map((refTp) -> new ReflectionTypeParameter(refTp, false)).collect(Collectors.toList());
    }

    @Override
    public ConstructorUsage resolveTypeVariables(Context context, List<TypeUsage> parameterTypes) {
        return new ConstructorUsage(new ReflectionConstructorDeclaration(constructor, typeSolver), typeSolver);
    }

    @Override
    public boolean isAbstract() {
        return ModifierSet.isAbstract(constructor.getModifiers());
    }

    @Override
    public boolean isPrivate() {
        return ModifierSet.isPrivate(constructor.getModifiers());
    }

    @Override
    public boolean isPackageProtected() {
        return !ModifierSet.isPrivate(constructor.getModifiers()) && !ModifierSet.isProtected(constructor.getModifiers()) && !ModifierSet.isPublic(constructor.getModifiers());
    }

}
