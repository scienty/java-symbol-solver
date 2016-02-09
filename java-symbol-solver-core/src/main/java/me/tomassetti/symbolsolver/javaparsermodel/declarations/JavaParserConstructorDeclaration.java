package me.tomassetti.symbolsolver.javaparsermodel.declarations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.expr.NameExpr;

import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFacade;
import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFactory;
import me.tomassetti.symbolsolver.model.declarations.ConstructorDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ParameterDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.invokations.ConstructorUsage;
import me.tomassetti.symbolsolver.model.invokations.MethodUsage;
import me.tomassetti.symbolsolver.model.resolution.Context;
import me.tomassetti.symbolsolver.model.resolution.TypeParameter;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.WildcardUsage;

public class JavaParserConstructorDeclaration implements ConstructorDeclaration {

    private com.github.javaparser.ast.body.ConstructorDeclaration wrappedNode;
    private TypeSolver typeSolver;

    public JavaParserConstructorDeclaration(com.github.javaparser.ast.body.ConstructorDeclaration wrappedNode, TypeSolver typeSolver) {
        this.wrappedNode = wrappedNode;
        this.typeSolver = typeSolver;
    }

    @Override
    public String toString() {
        return "JavaParserConstructorDeclaration{" +
                "wrappedNode=" + wrappedNode +
                ", typeSolver=" + typeSolver +
                '}';
    }

    @Override
    public TypeDeclaration declaringType() {
        if (wrappedNode.getParentNode() instanceof ClassOrInterfaceDeclaration) {
            return new JavaParserClassDeclaration((ClassOrInterfaceDeclaration) wrappedNode.getParentNode(), typeSolver);
        } else {
            throw new UnsupportedOperationException();
        }
    }
   
    @Override
    public List<TypeUsage> getExceptionTypes() {
    	try {
    		List<TypeUsage> retTypeUsage = new ArrayList<>();
    		JavaParserFacade parserFacade = JavaParserFacade.get(typeSolver);
    		for (NameExpr exNameExpr : wrappedNode.getThrows()) {
    			retTypeUsage.add(parserFacade.getType(exNameExpr));
			}
    		return retTypeUsage;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getNoParams() {
        if (wrappedNode.getParameters() == null) {
            return 0;
        }
        return wrappedNode.getParameters().size();
    }

    @Override
    public ParameterDeclaration getParam(int i) {
        return new JavaParserParameterDeclaration(wrappedNode.getParameters().get(i), typeSolver);
    }

    public MethodUsage getUsage(Node node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstructorUsage resolveTypeVariables(Context context, List<TypeUsage> parameterTypes) {
        List<TypeUsage> params = new ArrayList<>();
        for (int i = 0; i < wrappedNode.getParameters().size(); i++) {
            TypeUsage replaced = replaceTypeParams(new JavaParserConstructorDeclaration(wrappedNode, typeSolver).getParam(i).getType(), typeSolver, context);
            params.add(replaced);
        }

        // We now look at the type parameter for the method which we can derive from the parameter types
        // and then we replace them in the return type
        Map<String, TypeUsage> determinedTypeParameters = new HashMap<>();
        for (int i = 0; i < getNoParams(); i++) {
            TypeUsage formalParamType = getParam(i).getType();
            TypeUsage actualParamType = parameterTypes.get(i);
            determineTypeParameters(determinedTypeParameters, formalParamType, actualParamType, typeSolver);
        }

        return new ConstructorUsage(new JavaParserConstructorDeclaration(wrappedNode, typeSolver), params);
    }

    private void determineTypeParameters(Map<String, TypeUsage> determinedTypeParameters, TypeUsage formalParamType, TypeUsage actualParamType, TypeSolver typeSolver) {
        if (actualParamType.isNull()) {
            return;
        }
        if (actualParamType.isTypeVariable()) {
            return;
        }
        if (formalParamType.isTypeVariable()) {
            determinedTypeParameters.put(formalParamType.describe(), actualParamType);
            return;
        }
        if (formalParamType instanceof WildcardUsage) {
            return;
        }
        if (formalParamType.isReferenceType() && actualParamType.isReferenceType()
                && !formalParamType.asReferenceTypeUsage().getQualifiedName().equals(actualParamType.asReferenceTypeUsage().getQualifiedName())) {
            List<ReferenceTypeUsage> ancestors = actualParamType.asReferenceTypeUsage().getAllAncestors();
            final String formalParamTypeQName = formalParamType.asReferenceTypeUsage().getQualifiedName();
            List<TypeUsage> correspondingFormalType = ancestors.stream().filter((a) -> a.getQualifiedName().equals(formalParamTypeQName)).collect(Collectors.toList());
            if (correspondingFormalType.size() == 0) {
                throw new IllegalArgumentException();
            }
            actualParamType = correspondingFormalType.get(0);
        }
        if (formalParamType.isReferenceType() && actualParamType.isReferenceType()) {
            List<TypeUsage> formalTypeParams = formalParamType.asReferenceTypeUsage().parameters();
            List<TypeUsage> actualTypeParams = actualParamType.asReferenceTypeUsage().parameters();
            if (formalTypeParams.size() != actualTypeParams.size()) {
                throw new UnsupportedOperationException();
            }
            for (int i = 0; i < formalTypeParams.size(); i++) {
                determineTypeParameters(determinedTypeParameters, formalTypeParams.get(i), actualTypeParams.get(i), typeSolver);
            }
        }
    }

    private Context getContext() {
        return JavaParserFactory.getContext(wrappedNode, typeSolver);
    }

    @Override
    public boolean isAbstract() {
    	return ModifierSet.isAbstract(wrappedNode.getModifiers());
    }

    @Override
    public boolean isPrivate() {
    	return ModifierSet.isPrivate(wrappedNode.getModifiers());
    }

    private Optional<TypeUsage> typeParamByName(String name, TypeSolver typeSolver, Context context) {
        int i = 0;
        if (wrappedNode.getTypeParameters() != null) {
            for (com.github.javaparser.ast.TypeParameter tp : wrappedNode.getTypeParameters()) {
                if (tp.getName().equals(name)) {
                    TypeUsage typeUsage = JavaParserFacade.get(typeSolver).convertToUsage(this.wrappedNode.getParameters().get(i).getType(), context);
                    return Optional.of(typeUsage);
                }
                i++;
            }
        }
        return Optional.empty();
    }

    private TypeUsage replaceTypeParams(TypeUsage typeUsage, TypeSolver typeSolver, Context context) {
        if (typeUsage.isTypeVariable()) {
            TypeParameter typeParameter = typeUsage.asTypeParameter();
            if (typeParameter.declaredOnClass()) {
                Optional<TypeUsage> typeParam = typeParamByName(typeParameter.getName(), typeSolver, context);
                if (typeParam.isPresent()) {
                    typeUsage = typeParam.get();
                }
            }
        }

        if (typeUsage.isReferenceType()) {
            for (int i = 0; i < typeUsage.asReferenceTypeUsage().parameters().size(); i++) {
                TypeUsage replaced = replaceTypeParams(typeUsage.asReferenceTypeUsage().parameters().get(i), typeSolver, context);
                // Identity comparison on purpose
                if (replaced != typeUsage.asReferenceTypeUsage().parameters().get(i)) {
                    typeUsage = typeUsage.asReferenceTypeUsage().replaceParam(i, replaced);
                }
            }
        }

        return typeUsage;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TypeParameter> getTypeParameters() {
        if (this.wrappedNode.getTypeParameters() == null) {
            return Collections.emptyList();
        }
        return this.wrappedNode.getTypeParameters().stream().map((astTp) -> new JavaParserTypeParameter(astTp, typeSolver)).collect(Collectors.toList());
    }

    @Override
    public boolean isPackageProtected() {
    	return ModifierSet.isProtected(wrappedNode.getModifiers());
    }

	/**
	 * Returns the JavaParser node associated with this JavaParserMethodDeclaration.
	 *
	 * @return A visitable JavaParser node wrapped by this object.
	 */
	public com.github.javaparser.ast.body.ConstructorDeclaration getWrappedNode()
	{
		return wrappedNode;
	}
}
