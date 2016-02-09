package me.tomassetti.symbolsolver.resolution;

import java.util.List;
import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;

import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFactory;
import me.tomassetti.symbolsolver.javaparsermodel.UnsolvedSymbolException;
import me.tomassetti.symbolsolver.model.declarations.MethodDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.invokations.MethodUsage;
import me.tomassetti.symbolsolver.model.resolution.Context;
import me.tomassetti.symbolsolver.model.resolution.SymbolReference;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.resolution.Value;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsageImpl;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;

public class SymbolSolver {

    private TypeSolver typeSolver;

    public SymbolSolver(TypeSolver typeSolver) {
        if (typeSolver == null) throw new IllegalArgumentException();

        this.typeSolver = typeSolver;
    }

    public SymbolReference<? extends ValueDeclaration> solveSymbol(String name, Context context) {
        return context.solveSymbol(name, typeSolver);
    }

    public SymbolReference<? extends ValueDeclaration> solveSymbol(String name, Node node) {
        return solveSymbol(name, JavaParserFactory.getContext(node, typeSolver));
    }

    public Optional<Value> solveSymbolAsValue(String name, Context context) {
        return context.solveSymbolAsValue(name, typeSolver);
    }

    public Optional<Value> solveSymbolAsValue(String name, Node node) {
        Context context = JavaParserFactory.getContext(node, typeSolver);
        return solveSymbolAsValue(name, context);
    }

    public SymbolReference<? extends TypeDeclaration> solveType(String name, Context context) {
        return context.solveType(name, typeSolver);
    }

    public SymbolReference<? extends TypeDeclaration> solveType(String name, Node node) {
        return solveType(name, JavaParserFactory.getContext(node, typeSolver));
    }

    public MethodUsage solveMethod(String methodName, List<TypeUsage> params, Context context) {
        SymbolReference<MethodDeclaration> decl = context.solveMethod(methodName, params, typeSolver);
        if (!decl.isSolved()) {
            throw new UnsolvedSymbolException(context, methodName);
        }
        return new MethodUsage(decl.getCorrespondingDeclaration(), typeSolver);
    }

    public MethodUsage solveMethod(String methodName, List<TypeUsage> params, Node node) {
        return solveMethod(methodName, params, JavaParserFactory.getContext(node, typeSolver));
    }

    ;

    public TypeDeclaration solveType(Type type) {
        if (type instanceof ReferenceType) {
            ReferenceType referenceType = (ReferenceType) type;
            // TODO consider array modifiers
            return solveType(referenceType.getType());
        } else if (type instanceof ClassOrInterfaceType) {

            //should call typesolver here!

            /*if (0 == 0) throw new RuntimeException(type.getParentNode().getParentNode().toString());

            ClassOrInterfaceType classType = (ClassOrInterfaceType) type;
            SymbolReference<? extends ValueDeclaration> ref = solveSymbol(classType.getName(), type);
            if (!ref.isSolved()) {
                throw new UnsolvedSymbolException(JavaParserFactory.getContext(type), classType.getName());
            }
            if (!ref.getCorrespondingDeclaration().isType()) {
                throw new IllegalStateException(ref.getCorrespondingDeclaration().toString());
            }
            return ref.getCorrespondingDeclaration().asTypeDeclaration();*/
            String name = ((ClassOrInterfaceType) type).getName();
            SymbolReference<TypeDeclaration> ref = JavaParserFactory.getContext(type, typeSolver).solveType(name, typeSolver);
            if (!ref.isSolved()) {
                throw new UnsolvedSymbolException(JavaParserFactory.getContext(type, typeSolver), name);
            }
            return ref.getCorrespondingDeclaration();
        } else {
            throw new UnsupportedOperationException(type.getClass().getCanonicalName());
        }
    }

    public TypeUsage solveTypeUsage(String name, Context context) {
        Optional<TypeUsage> genericType = context.solveGenericType(name, typeSolver);
        if (genericType.isPresent()) {
            return genericType.get();
        }
        TypeDeclaration typeDeclaration = typeSolver.solveType(name);
        ReferenceTypeUsageImpl typeUsage = new ReferenceTypeUsageImpl(typeDeclaration, typeSolver);
        return typeUsage;
    }
}
