package me.tomassetti.symbolsolver.javaparsermodel.declarators;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.javaparser.ast.body.BaseParameter;

import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import me.tomassetti.symbolsolver.model.declarations.MethodDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;

public class ParameterSymbolDeclarator extends AbstractSymbolDeclarator<BaseParameter> {


    public ParameterSymbolDeclarator(BaseParameter wrappedNode, TypeSolver typeSolver) {
        super(wrappedNode, typeSolver);
    }

    @Override
    public List<ValueDeclaration> getSymbolDeclarations() {
        List<ValueDeclaration> symbols = new LinkedList<>();
        symbols.add(JavaParserSymbolDeclaration.parameter(wrappedNode, typeSolver));
        return symbols;
    }

    @Override
    public List<MethodDeclaration> getMethodDeclarations() {
        return Collections.emptyList();
    }
}
