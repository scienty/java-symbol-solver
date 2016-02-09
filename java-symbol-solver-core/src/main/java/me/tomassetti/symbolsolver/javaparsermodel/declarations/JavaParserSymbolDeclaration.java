package me.tomassetti.symbolsolver.javaparsermodel.declarations;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BaseParameter;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;

import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.PrimitiveTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;
import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFacade;
import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFactory;

public class JavaParserSymbolDeclaration implements ValueDeclaration {

    private String name;
    private Node wrappedNode;
    private boolean field;
    private boolean parameter;
    private boolean variable;
    private TypeSolver typeSolver;

    private JavaParserSymbolDeclaration(Node wrappedNode, String name, TypeSolver typeSolver, boolean field, boolean parameter, boolean variable) {
        this.name = name;
        this.wrappedNode = wrappedNode;
        this.field = field;
        this.variable = variable;
        this.parameter = parameter;
        this.typeSolver = typeSolver;
    }

    public static JavaParserSymbolDeclaration field(VariableDeclarator wrappedNode, TypeSolver typeSolver) {
        return new JavaParserSymbolDeclaration(wrappedNode, wrappedNode.getId().getName(), typeSolver, true, false, false);
    }

    public static JavaParserSymbolDeclaration parameter(BaseParameter parameter, TypeSolver typeSolver) {
        return new JavaParserSymbolDeclaration(parameter, parameter.getId().getName(), typeSolver, false, true, false);
    }

    public static JavaParserSymbolDeclaration localVar(VariableDeclarator variableDeclarator, TypeSolver typeSolver) {
        return new JavaParserSymbolDeclaration(variableDeclarator, variableDeclarator.getId().getName(), typeSolver, false, false, true);
    }

    public static int getParamPos(Parameter parameter) {
        int pos = 0;
        for (Node node : parameter.getParentNode().getChildrenNodes()) {
            if (node == parameter) {
                return pos;
            } else if (node instanceof Parameter) {
                pos++;
            }
        }
        return pos;
    }

    public static int getParamPos(Node node) {
        if (node.getParentNode() instanceof MethodCallExpr) {
            MethodCallExpr call = (MethodCallExpr) node.getParentNode();
            for (int i = 0; i < call.getArgs().size(); i++) {
                if (call.getArgs().get(i) == node) return i;
            }
            throw new IllegalStateException();
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return "JavaParserSymbolDeclaration{" +
                "name='" + name + '\'' +
                ", wrappedNode=" + wrappedNode +
                '}';
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isField() {
        return field;
    }

    @Override
    public boolean isParameter() {
        return parameter;
    }

    @Override
    public boolean isVariable() {
        return variable;
    }

    @Override
    public boolean isType() {
        return false;
    }

    @Override
    public TypeUsage getType() {
        if (wrappedNode instanceof Parameter) {
        	Parameter parameter = (Parameter) wrappedNode;
            if (wrappedNode.getParentNode() instanceof LambdaExpr) {
                int pos = getParamPos(parameter);
                TypeUsage lambdaType = JavaParserFacade.get(typeSolver).getType(wrappedNode.getParentNode());

                // TODO understand from the context to which method this corresponds
                //MethodDeclaration methodDeclaration = JavaParserFacade.get(typeSolver).getMethodCalled
                //MethodDeclaration methodCalled = JavaParserFacade.get(typeSolver).solve()
                throw new UnsupportedOperationException(wrappedNode.getClass().getCanonicalName());
            } else {
                if (parameter.getType() instanceof PrimitiveType) {
                    return PrimitiveTypeUsage.byName(((PrimitiveType) parameter.getType()).getType().name());
                } else {
                    return JavaParserFacade.get(typeSolver).convertToUsage(parameter.getType(), wrappedNode);
                }
            }
        } else if (wrappedNode instanceof MultiTypeParameter ) {
        	//there wont be any lambdaExpr in CatchBlock 
        	MultiTypeParameter parameter = (MultiTypeParameter) wrappedNode;
        	Type highestType = parameter.getTypes().get(0);
        	if ( highestType instanceof PrimitiveType) {
                return PrimitiveTypeUsage.byName(((PrimitiveType) highestType).getType().name());
            } else {
                return JavaParserFacade.get(typeSolver).convertToUsage(highestType, wrappedNode);
            }
        	
        } else if (wrappedNode instanceof VariableDeclarator) {
            VariableDeclarator variableDeclarator = (VariableDeclarator) wrappedNode;
            if (wrappedNode.getParentNode() instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) variableDeclarator.getParentNode();
                return JavaParserFacade.get(typeSolver).convert(variableDeclarationExpr.getType(), JavaParserFactory.getContext(wrappedNode, typeSolver));
            } else if (wrappedNode.getParentNode() instanceof FieldDeclaration) {
                FieldDeclaration fieldDeclaration = (FieldDeclaration) variableDeclarator.getParentNode();
                return JavaParserFacade.get(typeSolver).convert(fieldDeclaration.getType(), JavaParserFactory.getContext(wrappedNode, typeSolver));
            } else {
                throw new UnsupportedOperationException(wrappedNode.getParentNode().getClass().getCanonicalName());
            }
        } else {
            throw new UnsupportedOperationException(wrappedNode.getClass().getCanonicalName());
        }
    }

    /*@Override
    public TypeUsage getTypeUsage(TypeSolver typeSolver) {
        return JavaParserFacade.get(typeSolver).getType(wrappedNode);
    }*/

	/**
	 * Returns the JavaParser node associated with this JavaParserSymbolDeclaration.
	 *
	 * @return A visitable JavaParser node wrapped by this object.
	 */
	public Node getWrappedNode()
	{
		return wrappedNode;
	}
}
