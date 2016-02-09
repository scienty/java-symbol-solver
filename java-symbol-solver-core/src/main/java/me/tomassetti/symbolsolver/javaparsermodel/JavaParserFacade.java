package me.tomassetti.symbolsolver.javaparsermodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;

import javaslang.Tuple2;
import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserSymbolDeclaration;
import me.tomassetti.symbolsolver.javaparsermodel.declarations.JavaParserTypeVariableDeclaration;
import me.tomassetti.symbolsolver.logic.FunctionalInterfaceLogic;
import me.tomassetti.symbolsolver.logic.GenericTypeInferenceLogic;
import me.tomassetti.symbolsolver.model.declarations.MethodDeclaration;
import me.tomassetti.symbolsolver.model.declarations.TypeDeclaration;
import me.tomassetti.symbolsolver.model.declarations.ValueDeclaration;
import me.tomassetti.symbolsolver.model.invokations.MethodUsage;
import me.tomassetti.symbolsolver.model.resolution.Context;
import me.tomassetti.symbolsolver.model.resolution.SymbolReference;
import me.tomassetti.symbolsolver.model.resolution.TypeParameter;
import me.tomassetti.symbolsolver.model.resolution.TypeSolver;
import me.tomassetti.symbolsolver.model.typesystem.ArrayTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.NullTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.PrimitiveTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.ReferenceTypeUsageImpl;
import me.tomassetti.symbolsolver.model.typesystem.TypeParameterUsage;
import me.tomassetti.symbolsolver.model.typesystem.TypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.VoidTypeUsage;
import me.tomassetti.symbolsolver.model.typesystem.WildcardUsage;
import me.tomassetti.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import me.tomassetti.symbolsolver.resolution.SymbolSolver;
import me.tomassetti.symbolsolver.resolution.typesolvers.JreTypeSolver;

/**
 * Class to be used by final users to solve symbols for JavaParser ASTs.
 */
public class JavaParserFacade {

    private static Logger logger = Logger.getLogger(JavaParserFacade.class.getCanonicalName());
    static {
        logger.setLevel(Level.INFO);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
    }
    private static Map<TypeSolver, JavaParserFacade> instances = new HashMap<>();
    private TypeSolver typeSolver;
    private SymbolSolver symbolSolver;
    private Map<Node, TypeUsage> cacheWithLambdasSolved = new IdentityHashMap<>();
    private Map<Node, TypeUsage> cacheWithoutLambadsSolved = new IdentityHashMap<>();

    private JavaParserFacade(TypeSolver typeSolver) {
        this.typeSolver = typeSolver.getRoot();
        this.symbolSolver = new SymbolSolver(typeSolver);
    }

    public static JavaParserFacade get(TypeSolver typeSolver) {
        if (!instances.containsKey(typeSolver)) {
            instances.put(typeSolver, new JavaParserFacade(typeSolver));
        }
        return instances.get(typeSolver);
    }

    private static TypeUsage solveGenericTypes(TypeUsage typeUsage, Context context, TypeSolver typeSolver) {
        if (typeUsage.isTypeVariable()) {
            Optional<TypeUsage> solved = context.solveGenericType(typeUsage.describe(), typeSolver);
            if (solved.isPresent()) {
                return solved.get();
            } else {
                throw new UnsolvedSymbolException(context, typeUsage.describe());
            }
        } else if (typeUsage.isWildcard()) {
            if (typeUsage.asWildcard().isExtends() || typeUsage.asWildcard().isSuper()) {
                WildcardUsage wildcardUsage = typeUsage.asWildcard();
                TypeUsage boundResolved = solveGenericTypes(wildcardUsage.getBoundedType(), context, typeSolver);
                if (wildcardUsage.isExtends()) {
                    return WildcardUsage.extendsBound(boundResolved);
                } else {
                    return WildcardUsage.superBound(boundResolved);
                }
            } else {
                return typeUsage;
            }
        } else {
            TypeUsage result = typeUsage;
            int i = 0;
            if (result.isReferenceType()) {
                for (TypeUsage tp : typeUsage.asReferenceTypeUsage().parameters()) {
                    result = result.asReferenceTypeUsage().replaceParam(i, solveGenericTypes(tp, context, typeSolver));
                    i++;
                }
            }
            return result;
        }
    }

    public SymbolReference<? extends ValueDeclaration> solve(NameExpr nameExpr) {
        return symbolSolver.solveSymbol(nameExpr.getName(), nameExpr);
    }

    public SymbolReference solve(Expression expr) {
        if (expr instanceof NameExpr) {
            return solve((NameExpr) expr);
        } else {
            throw new IllegalArgumentException(expr.getClass().getCanonicalName());
        }
    }

    /**
     * Given a method call find out to which method declaration it corresponds.
     */
    public SymbolReference<MethodDeclaration> solve(MethodCallExpr methodCallExpr) {
        List<TypeUsage> params = new LinkedList<>();
        List<LambdaArgumentTypeUsagePlaceholder> placeholders = new LinkedList<>();
        int i = 0;
        for (Expression expression : methodCallExpr.getArgs()) {
            if (expression instanceof LambdaExpr) {
                LambdaArgumentTypeUsagePlaceholder placeholder = new LambdaArgumentTypeUsagePlaceholder(i);
                params.add(placeholder);
                placeholders.add(placeholder);
            } else {
                params.add(JavaParserFacade.get(typeSolver).getType(expression));
            }
            i++;
        }
        SymbolReference<MethodDeclaration> res = JavaParserFactory.getContext(methodCallExpr, typeSolver).solveMethod(methodCallExpr.getName(), params, typeSolver);
        for (LambdaArgumentTypeUsagePlaceholder placeholder : placeholders) {
            placeholder.setMethod(res);
        }
        return res;
    }

    public TypeUsage getType(Node node) {
        return getType(node, true);
    }

    public TypeUsage getType(Node node, boolean solveLambdas) {
        if (solveLambdas) {
            if (!cacheWithLambdasSolved.containsKey(node)) {
                TypeUsage res = getTypeConcrete(node, solveLambdas);

                cacheWithLambdasSolved.put(node, res);

                boolean secondPassNecessary = false;
                if (node instanceof MethodCallExpr) {
                    MethodCallExpr methodCallExpr = (MethodCallExpr)node;
                    for (Node arg : methodCallExpr.getArgs()) {
                        if (!cacheWithLambdasSolved.containsKey(arg)) {
                            getType(arg, true);
                            secondPassNecessary = true;
                        }
                    }
                }
                if (secondPassNecessary) {
                    cacheWithLambdasSolved.remove(node);
                    cacheWithLambdasSolved.put(node, getType(node, true));
                }
                logger.finer("getType on " + node + " -> " + res);
            }
            return cacheWithLambdasSolved.get(node);
        } else {
            Optional<TypeUsage> res = find(cacheWithLambdasSolved, node);
            if (res.isPresent()) {
                return res.get();
            }
            res = find(cacheWithoutLambadsSolved, node);
            if (!res.isPresent()) {
                TypeUsage resType = getTypeConcrete(node, solveLambdas);
                cacheWithoutLambadsSolved.put(node, resType);
                logger.finer("getType on " + node + " (no solveLambdas) -> " + res);
                return resType;
            }
            return res.get();
        }
    }

    private Optional<TypeUsage> find(Map<Node, TypeUsage> map, Node node) {
        if (map.containsKey(node)) {
            return Optional.of(map.get(node));
        }
        if (node instanceof LambdaExpr) {
            return find(map, (LambdaExpr)node);
        } else {
            return Optional.empty();
        }
    }

    /**
     * For some reasons LambdaExprs are duplicate and the equals method is not implemented correctly.
     * @param map
     * @return
     */
    private Optional<TypeUsage> find(Map<Node, TypeUsage> map, LambdaExpr lambdaExpr) {
        for (Node key : map.keySet()) {
            if (key instanceof LambdaExpr) {
                LambdaExpr keyLambdaExpr = (LambdaExpr)key;
                if (keyLambdaExpr.toString().equals(lambdaExpr.toString()) && keyLambdaExpr.getParentNode() == lambdaExpr.getParentNode()) {
                    return Optional.of(map.get(keyLambdaExpr));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Should return more like a TypeApplication: a TypeDeclaration and possible parameters or array modifiers.
     *
     * @return
     */
    private TypeUsage getTypeConcrete(Node node, boolean solveLambdas) {
        if (node == null) throw new IllegalArgumentException();
        if (node instanceof NameExpr) {
            NameExpr nameExpr = (NameExpr) node;
            logger.finest("getType on name expr " + node);
           
            try {
            Optional<me.tomassetti.symbolsolver.model.resolution.Value> value = new SymbolSolver(typeSolver).solveSymbolAsValue(nameExpr.getName(), nameExpr);
	            if (!value.isPresent()) {
	                throw new UnsolvedSymbolException("FOO Solving " + node, nameExpr.getName());
	            } else {
	                return value.get().getUsage();
	            }
            } catch (UnsolvedSymbolException e) {
                // Sure, it was not found as value because maybe it is a type and this is a static access
            	SymbolReference<TypeDeclaration> typeAccessedStatically = JavaParserFactory.getContext(nameExpr, typeSolver).solveType(nameExpr.toString(), typeSolver);
            	if (!typeAccessedStatically.isSolved()) {
            		throw e;
            	} else {
            		TypeDeclaration typeDeclaration = typeAccessedStatically.getCorrespondingDeclaration();
            		return new ReferenceTypeUsageImpl(typeDeclaration, typeSolver);
            	}

            } catch (Exception ex) {
            	System.out.println(ex);
            	throw ex;
            }
        } else if (node instanceof MethodCallExpr) {
            logger.finest("getType on method call " + node);
            // first solve the method
            MethodUsage ref = solveMethodAsUsage((MethodCallExpr) node);
            logger.finest("getType on method call " + node + " resolved to " + ref);
            logger.finest("getType on method call " + node + " return type is " + ref.returnType());
            return ref.returnType();
            // the type is the return type of the method
        } else if (node instanceof LambdaExpr) {
            if (node.getParentNode() instanceof MethodCallExpr) {
                MethodCallExpr callExpr = (MethodCallExpr) node.getParentNode();
                int pos = JavaParserSymbolDeclaration.getParamPos(node);
                SymbolReference<MethodDeclaration> refMethod = JavaParserFacade.get(typeSolver).solve(callExpr);
                if (!refMethod.isSolved()) {
                    throw new UnsolvedSymbolException(callExpr.getName());
                }
                logger.finest("getType on lambda expr " + refMethod.getCorrespondingDeclaration().getName());
                //logger.finest("Method param " + refMethod.getCorrespondingDeclaration().getParam(pos));
                if (solveLambdas) {
                    TypeUsage result = refMethod.getCorrespondingDeclaration().getParam(pos).getType();
                    // We need to replace the type variables
                    Context ctx = JavaParserFactory.getContext(node, typeSolver);
                    result = solveGenericTypes(result, ctx, typeSolver);

                    //We should find out which is the functional method (e.g., apply) and replace the params of the
                    //solveLambdas with it, to derive so the values. We should also consider the value returned by the
                    //lambdas
                    Optional<MethodUsage> functionalMethod = FunctionalInterfaceLogic.getFunctionalMethod(result);
                    if (functionalMethod.isPresent()) {
                        LambdaExpr lambdaExpr = (LambdaExpr)node;

                        List<Tuple2<TypeUsage, TypeUsage>> formalActualTypePairs = new ArrayList<>();
                        if (lambdaExpr.getBody() instanceof ExpressionStmt) {
                            ExpressionStmt expressionStmt = (ExpressionStmt)lambdaExpr.getBody();
                            TypeUsage actualType = getType (expressionStmt.getExpression());
                            TypeUsage formalType = functionalMethod.get().returnType();
                            formalActualTypePairs.add(new Tuple2<>(formalType, actualType));
                            Map<String, TypeUsage> inferredTypes = GenericTypeInferenceLogic.inferGenericTypes(formalActualTypePairs);
                            for (String typeName : inferredTypes.keySet()) {
                                result = result.replaceParam(typeName, inferredTypes.get(typeName));
                            }
                        } else {
                            throw new UnsupportedOperationException();
                        }
                    }

                    return result;
                } else {
                    return refMethod.getCorrespondingDeclaration().getParam(pos).getType();
                }
            } else {
                throw new UnsupportedOperationException("The type of a lambda expr depends on the position and its return value");
            }
        } else if (node instanceof VariableDeclarator) {
            if (node.getParentNode() instanceof FieldDeclaration) {
                FieldDeclaration parent = (FieldDeclaration) node.getParentNode();
                return JavaParserFacade.get(typeSolver).convertToUsage(parent.getType(), parent);
            } else if (node.getParentNode() instanceof VariableDeclarationExpr) {
                VariableDeclarationExpr parent = (VariableDeclarationExpr) node.getParentNode();
                return JavaParserFacade.get(typeSolver).convertToUsage(parent.getType(), parent);
            } else {
                throw new UnsupportedOperationException(node.getParentNode().getClass().getCanonicalName());
            }
        } else if (node instanceof Parameter) {
            Parameter parameter = (Parameter) node;
            if (parameter.getType() instanceof UnknownType) {
                throw new IllegalStateException("Parameter has unknown type: " + parameter);
            }
            return JavaParserFacade.get(typeSolver).convertToUsage(parameter.getType(), parameter);
        } else if (node instanceof FieldAccessExpr) {
            FieldAccessExpr fieldAccessExpr = (FieldAccessExpr) node;
            // We should understand if this is a static access
            try {
                Optional<me.tomassetti.symbolsolver.model.resolution.Value> value = new SymbolSolver(typeSolver).solveSymbolAsValue(fieldAccessExpr.getField(), fieldAccessExpr);
                if (value.isPresent()) {
                    return value.get().getUsage();
                } else {
                    throw new UnsolvedSymbolException(fieldAccessExpr.getField());
                }
            } catch (UnsolvedSymbolException e) {
                // Sure, it was not found as value because maybe it is a type and this is a static access
                if (fieldAccessExpr.getScope() instanceof NameExpr) {
                    NameExpr staticValue = (NameExpr) fieldAccessExpr.getScope();
                    SymbolReference<TypeDeclaration> typeAccessedStatically = JavaParserFactory.getContext(fieldAccessExpr, typeSolver).solveType(staticValue.toString(), typeSolver);
                    if (!typeAccessedStatically.isSolved()) {
                        throw e;
                    } else {
                        // TODO here maybe we have to substitute type parameters
                    	try {
                    		return typeAccessedStatically.getCorrespondingDeclaration().getField(fieldAccessExpr.getField()).getType();
                    	} catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
                    		//this again must be a nested static access
                        	SymbolReference<TypeDeclaration> fieldReference = typeAccessedStatically.getCorrespondingDeclaration().solveType(fieldAccessExpr.getField(), typeSolver);
                        	if ( fieldReference.isSolved() )
                        		return new ReferenceTypeUsageImpl(fieldReference.getCorrespondingDeclaration(), typeSolver);
                        	else 
                        		throw e;
                    	}
                    }
                } else {
                    throw e;
                }

            }
        } else if (node instanceof ObjectCreationExpr) {
            ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) node;
            TypeUsage typeUsage = JavaParserFacade.get(typeSolver).convertToUsage(objectCreationExpr.getType(), node);
            return typeUsage;
        } else if (node instanceof NullLiteralExpr) {
            return NullTypeUsage.INSTANCE;
        } else if (node instanceof BooleanLiteralExpr) {
            return PrimitiveTypeUsage.BOOLEAN;
        } else if (node instanceof IntegerLiteralExpr) {
            return PrimitiveTypeUsage.INT;
        } else if (node instanceof LongLiteralExpr) {
            return PrimitiveTypeUsage.LONG;
        } else if (node instanceof CharLiteralExpr) {
            return PrimitiveTypeUsage.CHAR;
        } else if (node instanceof StringLiteralExpr) {
            return new ReferenceTypeUsageImpl(new JreTypeSolver().solveType("java.lang.String"), typeSolver);
        } else if (node instanceof UnaryExpr) {
            UnaryExpr unaryExpr = (UnaryExpr) node;
            switch (unaryExpr.getOperator()) {
                case negative:
                case positive:
                    return getTypeConcrete(unaryExpr.getExpr(), solveLambdas);
                case not:
                    return PrimitiveTypeUsage.BOOLEAN;
                case posIncrement:
                case preIncrement:
                case preDecrement:
                case posDecrement:
                    return getTypeConcrete(unaryExpr.getExpr(), solveLambdas);
                default:
                    throw new UnsupportedOperationException(unaryExpr.getOperator().name());
            }
        } else if (node instanceof BinaryExpr) {
            BinaryExpr binaryExpr = (BinaryExpr) node;
            switch (binaryExpr.getOperator()) {
                case plus:
                case minus:
                    return getTypeConcrete(binaryExpr.getLeft(), solveLambdas);
                case lessEquals:
                case less:
                case greater:
                case greaterEquals:
                case equals:
                case notEquals:
                case or:
                case and:
                    return PrimitiveTypeUsage.BOOLEAN;
                case binAnd:
                case binOr:
                    return getTypeConcrete(binaryExpr.getLeft(), solveLambdas);
                default:
                    throw new UnsupportedOperationException("FOO " + binaryExpr.getOperator().name());
            }
        } else if (node instanceof VariableDeclarationExpr) {
            VariableDeclarationExpr expr = (VariableDeclarationExpr) node;
            return convertToUsage(expr.getType(), JavaParserFactory.getContext(node, typeSolver));
        } else if (node instanceof InstanceOfExpr) {
            return PrimitiveTypeUsage.BOOLEAN;
        } else if (node instanceof EnclosedExpr) {
            EnclosedExpr enclosedExpr = (EnclosedExpr) node;
            return getTypeConcrete(enclosedExpr.getInner(), solveLambdas);
        } else if (node instanceof CastExpr) {
            CastExpr enclosedExpr = (CastExpr) node;
            return convertToUsage(enclosedExpr.getType(), JavaParserFactory.getContext(node, typeSolver));
        } else if (node instanceof AssignExpr) {
            AssignExpr assignExpr = (AssignExpr) node;
            return getTypeConcrete(assignExpr.getTarget(), solveLambdas);
        } else if (node instanceof ThisExpr) {
            return new ReferenceTypeUsageImpl(getTypeDeclaration(findContainingTypeDecl(node)), typeSolver);
        } else if (node instanceof ConditionalExpr) {
            ConditionalExpr conditionalExpr = (ConditionalExpr) node;
            return getTypeConcrete(conditionalExpr.getThenExpr(), solveLambdas);
        } else if (node instanceof ArrayCreationExpr) {
            ArrayCreationExpr arrayCreationExpr = (ArrayCreationExpr) node;
            TypeUsage res = convertToUsage(arrayCreationExpr.getType(), JavaParserFactory.getContext(node, typeSolver));
            for (int i=0; i<arrayCreationExpr.getArrayCount();i++) {
                res = new ArrayTypeUsage(res);
            }
            return res;
        } else if (node instanceof ClassExpr) {
        	ClassExpr classExpr = (ClassExpr) node;
        	ReferenceTypeUsage typeUsage = convertToUsage(classExpr.getType(), node).asReferenceTypeUsage();
        	List<TypeUsage> typeParms = new ArrayList<>();
        	typeParms.add(typeUsage);
        	
        	return new ReferenceTypeUsageImpl(new ReflectionClassDeclaration(Class.class, typeSolver),  typeParms, typeSolver);
        } else {
            throw new UnsupportedOperationException(node.getClass().getCanonicalName());
        }
    }

    private com.github.javaparser.ast.body.TypeDeclaration findContainingTypeDecl(Node node) {
        if (node instanceof ClassOrInterfaceDeclaration) {
            return (ClassOrInterfaceDeclaration) node;
        } else if (node instanceof EnumDeclaration) {
            return (EnumDeclaration) node;
        } else if (node.getParentNode() == null) {
            throw new IllegalArgumentException();
        } else {
            return findContainingTypeDecl(node.getParentNode());
        }
    }

    public TypeUsage convertToUsage(Type type, Node context) {
        if (type instanceof UnknownType) {
            throw new IllegalArgumentException("Unknown type");
        }
        return convertToUsage(type, JavaParserFactory.getContext(context, typeSolver));
    }

    // This is an hack around an issue in JavaParser
    private String qName(ClassOrInterfaceType classOrInterfaceType) {
        String name = classOrInterfaceType.getName();
        if (classOrInterfaceType.getScope() != null) {
            return qName(classOrInterfaceType.getScope()) + "." + name;
        } else {
            return name;
        }
    }

    public TypeUsage convertToUsage(Type type, Context context) {
        if (type instanceof ReferenceType) {
            ReferenceType referenceType = (ReferenceType) type;
            TypeUsage typeUsage = convertToUsage(referenceType.getType(), context);
            for (int i = 0; i < referenceType.getArrayCount(); i++) {
                typeUsage = new ArrayTypeUsage(typeUsage);
            }
            return typeUsage;
        } else if (type instanceof ClassOrInterfaceType) {
            ClassOrInterfaceType classOrInterfaceType = (ClassOrInterfaceType) type;
            String name = qName(classOrInterfaceType);
            SymbolReference<TypeDeclaration> ref = context.solveType(name, typeSolver);
            if (!ref.isSolved()) {
                throw new UnsolvedSymbolException(name);
            }
            TypeDeclaration typeDeclaration = ref.getCorrespondingDeclaration();
            List<TypeUsage> typeParameters = Collections.emptyList();
            if (classOrInterfaceType.getTypeArgs() != null) {
                typeParameters = classOrInterfaceType.getTypeArgs().stream().map((pt) -> convertToUsage(pt, context)).collect(Collectors.toList());
            }
            if (typeDeclaration.isTypeVariable()) {
                if (typeDeclaration instanceof TypeParameter) {
                    return new TypeParameterUsage((TypeParameter) typeDeclaration);
                } else {
                    JavaParserTypeVariableDeclaration javaParserTypeVariableDeclaration = (JavaParserTypeVariableDeclaration) typeDeclaration;
                    return new TypeParameterUsage(javaParserTypeVariableDeclaration.asTypeParameter());
                }
            } else {
                return new ReferenceTypeUsageImpl(typeDeclaration, typeParameters, typeSolver);
            }
        } else if (type instanceof PrimitiveType) {
            return PrimitiveTypeUsage.byName(((PrimitiveType) type).getType().name());
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            if (wildcardType.getExtends() != null && wildcardType.getSuper() == null) {
                return WildcardUsage.extendsBound((ReferenceTypeUsageImpl) convertToUsage(wildcardType.getExtends(), context));
            } else if (wildcardType.getExtends() == null && wildcardType.getSuper() != null) {
                return WildcardUsage.extendsBound((ReferenceTypeUsageImpl) convertToUsage(wildcardType.getSuper(), context));
            } else if (wildcardType.getExtends() == null && wildcardType.getSuper() == null) {
                return WildcardUsage.UNBOUNDED;
            } else {
                throw new UnsupportedOperationException();
            }
        } else if (type instanceof VoidType) {
            return VoidTypeUsage.INSTANCE;
        } else {
            throw new UnsupportedOperationException(type.getClass().getCanonicalName());
        }
    }


    public TypeUsage convert(Type type, Node node) {
        return convert(type, JavaParserFactory.getContext(node, typeSolver));
    }

    public TypeUsage convert(Type type, Context context) {
        return convertToUsage(type, context);
    }

    public MethodUsage solveMethodAsUsage(MethodCallExpr call) {
        List<TypeUsage> params = new ArrayList<>();
        if (call.getArgs() != null) {
            for (Expression param : call.getArgs()) {
                //getTypeConcrete(Node node, boolean solveLambdas)
                params.add(getType(param, false));
                //params.add(getTypeConcrete(param, false));
            }
        }
        Context context = JavaParserFactory.getContext(call, typeSolver);
        Optional<MethodUsage> methodUsage = context.solveMethodAsUsage(call.getName(), params, typeSolver);
        if (!methodUsage.isPresent()) {
            throw new RuntimeException("Method '" + call.getName() + "' cannot be resolved in context "
                    + call + " (line: " + call.getBeginLine() + ") " + context);
        }
        return methodUsage.get();
    }

    public TypeDeclaration getTypeDeclaration(ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {
        if (classOrInterfaceDeclaration.isInterface()) {
            return new JavaParserInterfaceDeclaration(classOrInterfaceDeclaration, typeSolver);
        } else {
            return new JavaParserClassDeclaration(classOrInterfaceDeclaration, typeSolver);
        }
    }

    /**
     * "this" inserted in the given point, which type would have?
     */
    public TypeUsage getTypeOfThisIn(Node node) {
        // TODO consider static methods
        if (node instanceof ClassOrInterfaceDeclaration) {
            JavaParserClassDeclaration classDeclaration = new JavaParserClassDeclaration((ClassOrInterfaceDeclaration) node, typeSolver);
            return new ReferenceTypeUsageImpl(classDeclaration, typeSolver);
        } else {
            return getTypeOfThisIn(node.getParentNode());
        }
    }

    public TypeDeclaration getTypeDeclaration(com.github.javaparser.ast.body.TypeDeclaration typeDeclaration) {
        if (typeDeclaration instanceof ClassOrInterfaceDeclaration) {
            return getTypeDeclaration((ClassOrInterfaceDeclaration) typeDeclaration);
        } else if (typeDeclaration instanceof EnumDeclaration) {
            return new JavaParserEnumDeclaration((EnumDeclaration) typeDeclaration, typeSolver);
        } else {
            throw new UnsupportedOperationException(typeDeclaration.getClass().getCanonicalName());
        }
    }
}
