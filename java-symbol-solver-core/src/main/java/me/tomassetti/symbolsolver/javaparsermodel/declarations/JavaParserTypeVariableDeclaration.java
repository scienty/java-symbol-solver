package me.tomassetti.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.TypeParameter;
import me.tomassetti.symbolsolver.logic.AbstractTypeDeclaration;
import me.tomassetti.symbolsolver.model.declarations.FieldDeclaration;
import me.tomassetti.symbolsolver.model.declarations.MethodDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.resolution.Context;
import me.tomassetti.symbolsolver.model.resolution.SymbolReference;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsageImpl;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JavaParserTypeVariableDeclaration extends AbstractTypeDeclaration {

    private TypeParameter wrappedNode;
    private TypeSolver typeSolver;

    public JavaParserTypeVariableDeclaration(TypeParameter wrappedNode, TypeSolver typeSolver) {
        this.wrappedNode = wrappedNode;
        this.typeSolver = typeSolver;
    }

    @Override
    public boolean isAssignableBy(TypeDeclaration other) {
        return isAssignableBy(new ReferenceTypeUsageImpl(other, typeSolver));
    }

    @Override
    public String getQualifiedName() {
        return getName();
    }

    @Override
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "JavaParserTypeVariableDeclaration{" +
                wrappedNode.getName() +
                '}';
    }

    @Override
    public SymbolReference<MethodDeclaration> solveMethod(String name, List<TypeUsage> parameterTypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected TypeSolver typeSolver() {
        return typeSolver;
    }

    public TypeUsage getUsage(Node node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignableBy(TypeUsage typeUsage) {
        if (typeUsage.isTypeVariable()) {
            throw new UnsupportedOperationException("Is this type variable declaration assignable by " + typeUsage.describe());
        } else {
            throw new UnsupportedOperationException("Is this type variable declaration assignable by " + typeUsage);
        }
    }

    @Override
    public boolean isTypeVariable() {
        return true;
    }

    @Override
    public FieldDeclaration getField(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasField(String name) {
        return false;
    }

    @Override
    public SymbolReference<? extends ValueDeclaration> solveSymbol(String substring, TypeSolver typeSolver) {
        return SymbolReference.unsolved(ValueDeclaration.class);
    }

    @Override
    public SymbolReference<TypeDeclaration> solveType(String substring, TypeSolver typeSolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ReferenceTypeUsage> getAllAncestors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<MethodDeclaration> getDeclaredMethods() {
        return Collections.emptySet();
    }

    @Override
    public String getName() {
        return wrappedNode.getName();
    }

    @Override
    public boolean isField() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isParameter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public boolean isType() {
        return true;
    }

    @Override
    public boolean isClass() {
        return false;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public List<me.tomassetti.symbolsolver.model.resolution.TypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    public me.tomassetti.symbolsolver.model.resolution.TypeParameter asTypeParameter() {
        return new JavaParserTypeParameter(this.wrappedNode, typeSolver);
    }

	/**
	 * Returns the JavaParser node associated with this JavaParserTypeVariableDeclaration.
	 *
	 * @return A visitable JavaParser node wrapped by this object.
	 */
	public TypeParameter getWrappedNode()
	{
		return wrappedNode;
	}

	@Override
	public List<FieldDeclaration> getAllFields() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<FieldDeclaration> getDeclaredFields() {
		throw new UnsupportedOperationException();
	}
}
