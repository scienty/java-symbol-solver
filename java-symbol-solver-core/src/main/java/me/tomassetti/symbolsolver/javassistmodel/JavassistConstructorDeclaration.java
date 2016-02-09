package me.tomassetti.symbolsolver.javassistmodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ModifierSet;

import javassist.CtConstructor;
import javassist.NotFoundException;
import javassist.bytecode.BadBytecode;
import javassist.bytecode.SignatureAttribute;
import me.tomassetti.symbolsolver.model.declarations.ConstructorDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ParameterDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.invokations.ConstructorUsage;
import me.tomassetti.symbolsolver.model.resolution.Context;
import me.tomassetti.symbolsolver.model.resolution.TypeParameter;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;

public class JavassistConstructorDeclaration implements ConstructorDeclaration {
    private CtConstructor ctConstructor;
    private TypeSolver typeSolver;
    public JavassistConstructorDeclaration(CtConstructor ctConstructor, TypeSolver typeSolver) {
        this.ctConstructor = ctConstructor;
        this.typeSolver = typeSolver;
    }

    @Override
    public String toString() {
        return "JavassistConstructorDeclaration{" +
                "ctConstructor=" + ctConstructor +
                '}';
    }

    @Override
    public String getName() {
        return ctConstructor.getName();
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
    public boolean isType() {
        return false;
    }

    @Override
    public TypeDeclaration declaringType() {
        if (ctConstructor.getDeclaringClass().isInterface()) {
            return new JavassistInterfaceDeclaration(ctConstructor.getDeclaringClass(), typeSolver);
        } else {
            return new JavassistClassDeclaration(ctConstructor.getDeclaringClass(), typeSolver);
        }
    }
    
    @Override
    public List<TypeUsage> getExceptionTypes() {
    	try {
    		return Arrays.stream(ctConstructor.getExceptionTypes()).map((ctExType) -> JavassistFactory.typeUsageFor(ctExType, typeSolver)).collect(Collectors.toList());
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public int getNoParams() {
        try {
            return ctConstructor.getParameterTypes().length;
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ParameterDeclaration getParam(int i) {
        try {
            boolean variadic = false;
            if ((ctConstructor.getModifiers() & javassist.Modifier.VARARGS) > 0) {
                variadic = i == (ctConstructor.getParameterTypes().length - 1);
            }
            return new JavassistParameterDeclaration(ctConstructor.getParameterTypes()[i], typeSolver, variadic);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public ConstructorUsage getUsage(Node node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstructorUsage resolveTypeVariables(Context context, List<TypeUsage> parameterTypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        return ModifierSet.isAbstract(ctConstructor.getModifiers());
    }

    @Override
    public boolean isPrivate() {
        return ModifierSet.isPrivate(ctConstructor.getModifiers());
    }

    @Override
    public boolean isPackageProtected() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TypeParameter> getTypeParameters() {
        try {
            if (ctConstructor.getGenericSignature() == null) {
                return Collections.emptyList();
            }
            SignatureAttribute.MethodSignature methodSignature = SignatureAttribute.toMethodSignature(ctConstructor.getGenericSignature());
            return Arrays.stream(methodSignature.getTypeParameters()).map((jasTp) -> new JavassistTypeParameter(jasTp, false, typeSolver)).collect(Collectors.toList());
        } catch (BadBytecode badBytecode) {
            throw new RuntimeException(badBytecode);
        }
    }
}
