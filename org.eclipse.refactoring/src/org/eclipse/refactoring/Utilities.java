package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.refactoring.LocalVariableCollector.Tuple;
import org.eclipse.text.edits.TextEdit;

public class Utilities {
	private static final String PUSH = "push";
	static final String RET = "ret";
	private static final String SECTION = "section";
	public static final String CONTEXT_VAR = "context";
	private static final String CONTEXT = "Context";
	private static final String ITERATIVE = "Iterative";
	private static final String DEQUE = "Deque";
	private static final String LINKED_LIST = "LinkedList";
	private static final String JAVA_UTIL_LINKED_LIST = "java.util." + LINKED_LIST;
	private static final String JAVA_UTIL_DEQUE = "java.util." + DEQUE;
	static final String PEEK = "peek";
	static final String STACK = "stack";

	public static boolean isRecursive(IMethod method) throws CoreException {
		IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] { method });
		SearchPattern pattern = SearchPattern.createPattern(method, IJavaSearchConstants.REFERENCES,
				SearchPattern.R_EXACT_MATCH);
		SearchEngine engine = new SearchEngine();
		final Set<SearchMatch> invocations = new HashSet<SearchMatch>();

		engine.search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() }, scope,
				new SearchRequestor() {

					@Override
					public void acceptSearchMatch(SearchMatch match) throws CoreException {
						if (match.getAccuracy() == SearchMatch.A_ACCURATE && !match.isInsideDocComment())
							invocations.add(match);
					}
				}, null);
		return !invocations.isEmpty();
	}

	public static void simplifyIfStatements(AST ast, List<Statement> statements) {
		if (statements.size() != 1 || !(statements.get(0) instanceof IfStatement)) {
			return;
		}
		IfStatement ifStatement = (IfStatement) statements.get(0);

		Statement thenStatement = ifStatement.getThenStatement();
		if (!(thenStatement instanceof Block) && !(thenStatement instanceof ReturnStatement)) {
			Block block = ast.newBlock();
			List<Statement> statements2 = block.statements();
			statements2.add(copySubtree(ast, thenStatement));
			statements2.add(ast.newReturnStatement());

			ifStatement.setThenStatement(block);
		} else if (thenStatement instanceof Block) {
			Block block = (Block) thenStatement;
			List<Statement> statements2 = block.statements();
			if (!(statements2.get(statements2.size() - 1) instanceof ReturnStatement)) {
				statements2.add(ast.newReturnStatement());
			}
		} else {
			Block block = ast.newBlock();
			block.statements().add(copySubtree(ast, thenStatement));

			ifStatement.setThenStatement(block);
		}

		Statement elseStatement = ifStatement.getElseStatement();
		ifStatement.setElseStatement(null);

		if (elseStatement instanceof Block) {
			Block block = (Block) elseStatement;
			for (Statement statement : (List<Statement>) block.statements()) {
				statements.add(copySubtree(ast, statement));
			}
			if (!(statements.get(statements.size() - 1) instanceof ReturnStatement)) {
				statements.add(ast.newReturnStatement());
			}
		} else {
			statements.add(elseStatement);
		}
	}

	public static Statement getParentStatement(MethodInvocation invocation) {
		ASTNode parent = invocation.getParent();
		while (parent != null) {
			if (parent instanceof Statement) {
				return (Statement) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	public static Block getParentBlock(MethodInvocation invocation) {
		ASTNode parent = invocation.getParent();
		while (parent != null) {
			if (parent instanceof Block) {
				return (Block) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	public static int getIndexOfStatementWithInvocation(MethodInvocation invocation) {
		Block block = getParentBlock(invocation);
		Statement parentStatement = getParentStatement(invocation);
		List<Statement> statements = block.statements();
		for (int i = 0; i < statements.size(); i++) {
			if (statements.get(i) == parentStatement) {
				return i;
			}
		}
		return -1;
	}

	private static List<MethodInvocation> getInvocations(String methodName, MethodDeclaration method) {
		MethodInvocationsCollector invocationsCollector = new MethodInvocationsCollector(methodName);
		method.accept(invocationsCollector);
		return invocationsCollector.getMethodInvocations();
	}

	public static void extractRecursiveMethodInvocationsToLocalVariables(IMethod method, ASTNode astNode,
			MethodDeclaration newMethod, AST ast) throws JavaModelException {
		List<MethodInvocation> invocations = getInvocations(method.getElementName(), newMethod);
		System.out.println(invocations);
		for (MethodInvocation invocation : invocations) {
			if (invocation.getParent() != getParentStatement(invocation)) {
				System.out.println(invocation + " is not in immediate statement.");
				int index = getIndexOfStatementWithInvocation(invocation);
				Block block = getParentBlock(invocation);
				block.statements().add(index, ast.newExpressionStatement(copySubtree(ast, invocation)));
				invocation.getParent().setStructuralProperty(invocation.getLocationInParent(), ast.newSimpleName(RET));
				// See ASTResolving
			}
		}
	}

	public static List<List<Statement>> getSections(String methodName, MethodDeclaration method) {
		List<MethodInvocation> invocations = getInvocations(methodName, method);
		Block block = method.getBody();
		List<List<Statement>> sections = new ArrayList<>();
		int lastIndex = 0;

		for (MethodInvocation invocation : invocations) {
			Statement statement = getParentStatement(invocation);
			if (statement.getParent() == block) {
				int index = block.statements().indexOf(statement);
				System.out.println("Split at " + index);
				List<Statement> statements = new ArrayList<Statement>();
				statements.addAll(block.statements().subList(lastIndex, index + 1));
				sections.add(statements);
				lastIndex = index + 1;
			}
		}

		List<Statement> statements = new ArrayList<Statement>();
		statements.addAll(block.statements().subList(lastIndex, block.statements().size()));
		sections.add(statements);

		return sections;
	}

	private static void addIfNotPresent(ListRewrite listRewrite, ImportDeclaration importDeclaration,
			List<ImportDeclaration> list) {
		boolean isPresent = false;
		for (ImportDeclaration declaration : list) {
			if (declaration.getName().getFullyQualifiedName()
					.equals(importDeclaration.getName().getFullyQualifiedName())) {
				isPresent = true;
				break;
			}
		}

		if (!isPresent) {
			listRewrite.insertLast(importDeclaration, null);
		}
	}

	private static void addImports(AST ast, ASTNode astNode, ASTRewrite astRewrite) {
		ImportDeclaration dequeImport = ast.newImportDeclaration();
		dequeImport.setName(ast.newName(JAVA_UTIL_DEQUE));

		ImportDeclaration listImport = ast.newImportDeclaration();
		listImport.setName(ast.newName(JAVA_UTIL_LINKED_LIST));

		ListRewrite rewrite = astRewrite.getListRewrite(astNode, CompilationUnit.IMPORTS_PROPERTY);
		List<ImportDeclaration> imports = rewrite.getOriginalList();

		addIfNotPresent(rewrite, dequeImport, imports);
		addIfNotPresent(rewrite, listImport, imports);
	}

	public static Change createContextClass(IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		ASTParser parser = createParser(method);
		ASTNode astNode = parser.createAST(null);
		AST ast = astNode.getAST();
		ASTRewrite astRewrite = ASTRewrite.create(ast);

		MethodDeclaration oldMethod = (MethodDeclaration) NodeFinder.perform(astNode, method.getSourceRange());

		ASTNode declaringTypeNode = NodeFinder.perform(astNode, declaringType.getSourceRange());

		addImports(ast, astNode, astRewrite);

		ListRewrite listRewrite = astRewrite.getListRewrite(declaringTypeNode,
				TypeDeclaration.BODY_DECLARATIONS_PROPERTY);

		MethodDeclaration newMethod = createMethodDeclaration(astNode, oldMethod);

		LocalVariableCollector visitor = new LocalVariableCollector();
		oldMethod.accept(visitor);
		List<Tuple> tuples = visitor.getVariableDeclarations();

		Block body = newMethod.getBody();

		List<Statement> statements = body.statements();

		simplifyIfStatements(ast, statements);
		extractRecursiveMethodInvocationsToLocalVariables(method, astNode, newMethod, ast);
		body.accept(new DeclarationWithInitializerToAsssignmentReplacer());

		String methodName = method.getElementName();
		String contextName = getContextName(methodName);

		listRewrite.insertLast(createTypeDeclaration(contextName, ast, tuples), null);

		List<List<Statement>> sections = getSections(methodName, newMethod);

		statements.clear();
		Type contextType = getContextType(ast, methodName);
		
		statements.add(createStackDeclarationStatement(contextType, ast));
		statements.add(createPushInvocation(contextType, ast, newMethod.parameters()));
		addRetDeclaration(ast, newMethod.getReturnType2(), statements);
		statements.add(createWhileStatement(method, ast, sections, tuples, contextType));

		listRewrite.insertLast(newMethod, null);

		TextEdit edit = astRewrite.rewriteAST();
		ICompilationUnit unit = method.getCompilationUnit();

		TextFileChange change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
		change.setTextType("java");
		change.setEdit(edit);

		return change;
	}

	private static VariableDeclarationStatement newVariableDeclarationStatement(AST ast,
			VariableDeclarationFragment fragment, Type type) {
		VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
		statement.setType(type);
		return statement;
	}

	private static void addRetDeclaration(AST ast, Type type, List<Statement> statements) {
		if (type instanceof PrimitiveType && ((PrimitiveType) type).getPrimitiveTypeCode() == PrimitiveType.VOID)
			return;
		VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast, ast.newSimpleName(RET),
				ast.newNumberLiteral());
		statements.add(newVariableDeclarationStatement(ast, fragment, copySubtree(ast, type)));
	}

	private static boolean inTuples(List<Tuple> tuples, String name) {
		for (Tuple tuple : tuples) {
			if (tuple.variableDeclaration.getName().getFullyQualifiedName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private static void replaceSimpleNameWithContextAccess(SimpleName node, List<Tuple> tuples) {
		String name = node.getFullyQualifiedName();
		if (inTuples(tuples, name)) {
			ASTNode parent = node.getParent();
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			AST ast = node.getAST();
			FieldAccess fieldAccess = newFieldAccess(ast, ast.newSimpleName(CONTEXT_VAR), ast.newSimpleName(name));
			if (parent instanceof MethodInvocation) {
				MethodInvocation invocation = (MethodInvocation) parent;
				List<Expression> arguments = invocation.arguments();
				for (Expression expression : arguments) {
					if (expression == node) {
						int index = arguments.indexOf(expression);
						arguments.remove(expression);
						arguments.add(index, fieldAccess);
					}
				}
			} else {
				parent.setStructuralProperty(locationInParent, fieldAccess);
			}
		}
	}

	private static Statement createStackPushNewContextStatement(AST ast, Type contextType, List<Expression> arguments) {
		List<Expression> newArguments = new ArrayList<>();
		for (Expression argument : arguments) {
			newArguments.add(copySubtree(ast, argument));
		}
		ClassInstanceCreation creation = newClassInstanceCreation(ast, copySubtree(ast, contextType), newArguments);

		List<Expression> args = new ArrayList<>();
		args.add(creation);
		MethodInvocation invocation = newMethodInvocation(ast, ast.newSimpleName(STACK), ast.newSimpleName(PUSH), args);

		return ast.newExpressionStatement(invocation);
	}

	private static List<Statement> replaceInvocationWithStatements(AST ast, Type contextType, List<Tuple> tuples,
			MethodInvocation invocation) {
		List<Statement> statements = new ArrayList<>();
		statements.add(createStackPushNewContextStatement(ast, contextType, invocation.arguments()));
		statements.add(ast.newBreakStatement());
		return statements;
	}

	private static void replaceInvocationWithStatements2(AST ast, Type contextType, List<Tuple> tuples,
			MethodInvocation invocation) {
		List<Statement> statements = replaceInvocationWithStatements(ast, contextType, tuples, invocation);
		Statement parentStatement = getParentStatement(invocation);
		Block parentBlock = getParentBlock(invocation);
		List<Statement> blockStatements = parentBlock.statements();
		for (int i = 0; i < blockStatements.size(); i++) {
			Statement statement = blockStatements.get(i);
			if (statement == parentStatement) {
				blockStatements.remove(i);
				blockStatements.addAll(i, statements);
			}
		}
	}

	private static Statement createContextIncrementStatement(AST ast) {
		FieldAccess access = newFieldAccess(ast, ast.newSimpleName(CONTEXT_VAR), ast.newSimpleName(SECTION));
		Assignment assignment = newAssignment(ast, access, ast.newNumberLiteral("1"));
		assignment.setOperator(Operator.PLUS_ASSIGN);
		return ast.newExpressionStatement(assignment);
	}

	private static WhileStatement createWhileStatement(IMethod method, AST ast, List<List<Statement>> sections,
			List<Tuple> tuples, Type contextType) {
		SwitchStatement statement = ast.newSwitchStatement();
		statement.setExpression(newFieldAccess(ast, ast.newSimpleName(CONTEXT_VAR), ast.newSimpleName(SECTION)));
		List<Statement> statements = statement.statements();

		for (int i = 0; i < sections.size(); i++) {
			List<Statement> section = sections.get(i);

			SwitchCase switchCase = ast.newSwitchCase();
			switchCase.setExpression(ast.newNumberLiteral(Integer.toString(i)));
			statements.add(switchCase);

			Block block = ast.newBlock();
			List<Statement> statements2 = block.statements();

			statements2.add(createContextIncrementStatement(ast));

			for (Statement statement2 : section) {
				statements2.add(copySubtree(ast, statement2));
			}

			LocalVariableReplacer2 replacer2 = new LocalVariableReplacer2();
			block.accept(replacer2);

			List<SimpleName> simpleNames = replacer2.getSimpleNames();
			for (SimpleName simpleName : simpleNames) {
				replaceSimpleNameWithContextAccess(simpleName, tuples);
			}

			ReturnReplacer replacer3 = new ReturnReplacer();
			block.accept(replacer3);

			MethodInvocationsCollector collector = new MethodInvocationsCollector(method.getElementName());
			block.accept(collector);
			List<MethodInvocation> invocations = collector.getMethodInvocations();
			for (MethodInvocation invocation : invocations) {
				replaceInvocationWithStatements2(ast, contextType, tuples, invocation);
			}

			if (!(statements2.get(statements2.size() - 1) instanceof BreakStatement)) {
				statements2.add(ast.newBreakStatement());
			}

			statements.add(block);
		}

		SwitchCase defaultCase = ast.newSwitchCase();
		defaultCase.setExpression(null);
		statements.add(defaultCase);

		statements.add(ast.newBreakStatement());

		Block block = ast.newBlock();
		List<Statement> statements2 = block.statements();
		statements2.add(createStackPeek(contextType, ast));
		statements2.add(statement);

		WhileStatement whileStatement = ast.newWhileStatement();
		whileStatement.setExpression(ast.newBooleanLiteral(true));
		whileStatement.setBody(block);

		return whileStatement;
	}

	private static Type getContextType(AST ast, String methodName) {
		return ast.newSimpleType(ast.newSimpleName(getContextName(methodName)));
	}

	private static VariableDeclarationStatement createStackPeek(Type contextType, AST ast) {
		MethodInvocation invocation = newMethodInvocation(ast, ast.newSimpleName(STACK), ast.newSimpleName(PEEK), null);
		VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast, ast.newSimpleName(CONTEXT_VAR),
				invocation);
		VariableDeclarationStatement statement = newVariableDeclarationStatement(ast, fragment,
				copySubtree(ast, contextType));
		return statement;
	}

	static MethodInvocation newMethodInvocation(AST ast, Expression expression, SimpleName name,
			List<Expression> arguments) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(expression);
		invocation.setName(name);
		if (arguments != null) {
			invocation.arguments().addAll(arguments);
		}
		return invocation;
	}

	private static ClassInstanceCreation newClassInstanceCreation(AST ast, Type type, List<Expression> arguments) {
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(type);
		if (arguments != null) {
			creation.arguments().addAll(arguments);
		}
		return creation;
	}

	private static List<Expression> createContextArgs(AST ast, List<SingleVariableDeclaration> parameters) {
		return parameters.stream().map(parameter -> copySubtree(ast, parameter.getName())).collect(Collectors.toList());
	}

	private static Statement createPushInvocation(Type contextType, AST ast, List<SingleVariableDeclaration> parameters) {
		ClassInstanceCreation creation = newClassInstanceCreation(ast, copySubtree(ast, contextType),
				createContextArgs(ast, parameters));

		List<Expression> invocationArgs = new ArrayList<>();
		invocationArgs.add(creation);
		MethodInvocation invocation = newMethodInvocation(ast, ast.newSimpleName(STACK), ast.newSimpleName(PUSH),
				invocationArgs);

		return ast.newExpressionStatement(invocation);
	}

	private static Statement createStackDeclarationStatement(Type contextType, AST ast) {
		ClassInstanceCreation creation = newClassInstanceCreation(ast,
				ast.newParameterizedType(ast.newSimpleType(ast.newName(LINKED_LIST))), null);
		VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast, ast.newSimpleName(STACK), creation);

		ParameterizedType dequeType = ast.newParameterizedType(ast.newSimpleType(ast.newName(DEQUE)));
		dequeType.typeArguments().add(copySubtree(ast, contextType));

		return newVariableDeclarationStatement(ast, fragment, dequeType);
	}

	private static MethodDeclaration createMethodDeclaration(ASTNode astNode, MethodDeclaration oldMethod)
			throws JavaModelException {
		AST ast = astNode.getAST();
		MethodDeclaration newMethod = copySubtree(ast, oldMethod);
		newMethod.setName(ast.newSimpleName(newMethod.getName().getFullyQualifiedName() + ITERATIVE));
		return newMethod;
	}

	private static String capitalize(String input) {
		return input.substring(0, 1).toUpperCase() + input.substring(1);
	}

	private static String getContextName(String methodName) {
		return capitalize(methodName) + CONTEXT;
	}

	private static TypeDeclaration createTypeDeclaration(String contextName, AST ast, List<Tuple> tuples)
			throws JavaModelException {
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(contextName));

		List<Modifier> modifiers = typeDeclaration.modifiers();
		modifiers.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		modifiers.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		bodyDeclarations.addAll(createFields(ast, tuples));
		bodyDeclarations.add(createConstructor(ast, contextName, tuples));

		return typeDeclaration;
	}

	private static VariableDeclarationFragment newVariableDeclarationFragment(AST ast, SimpleName name,
			Expression expression) {
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(name);
		fragment.setInitializer(expression);
		return fragment;
	}

	private static FieldDeclaration newFieldDeclaration(AST ast, VariableDeclarationFragment fragment, Type type) {
		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
		fieldDeclaration.setType(type);
		return fieldDeclaration;
	}

	private static List<FieldDeclaration> createFields(AST ast, List<Tuple> tuples) {
		List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();

		for (Tuple tuple : tuples) {
			VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast,
					copySubtree(ast, tuple.variableDeclaration.getName()), null);
			FieldDeclaration fieldDeclaration = newFieldDeclaration(ast, fragment, copySubtree(ast, tuple.type));
			fields.add(fieldDeclaration);
		}

		VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast, ast.newSimpleName(SECTION), null);
		FieldDeclaration fieldDeclaration = newFieldDeclaration(ast, fragment, ast.newPrimitiveType(PrimitiveType.INT));
		fields.add(fieldDeclaration);

		return fields;
	}

	private static <T extends ASTNode> T createCopyTarget(ASTRewrite astRewrite, T node) {
		return (T) astRewrite.createCopyTarget(node);
	}

	public static <T extends ASTNode> T copySubtree(AST target, T node) {
		return (T) ASTNode.copySubtree(target, node);
	}

	private static MethodDeclaration newConstructor(AST ast, SimpleName name,
			List<SingleVariableDeclaration> parameters, Block body) {
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setConstructor(true);
		constructor.setName(name);
		constructor.parameters().addAll(parameters);
		constructor.setBody(body);
		return constructor;
	}

	private static BodyDeclaration createConstructor(AST ast, String name, List<Tuple> tuples) {
		return newConstructor(ast, ast.newSimpleName(name), createConstructorParameters(ast, tuples),
				createConstructorBody(ast, tuples));
	}

	private static SingleVariableDeclaration newSingleVariableDeclaration(AST ast, SimpleName name, Type type) {
		SingleVariableDeclaration declaration = ast.newSingleVariableDeclaration();
		declaration.setName(name);
		declaration.setType(type);
		return declaration;
	}

	private static List<SingleVariableDeclaration> createConstructorParameters(AST ast, List<Tuple> tuples) {
		List<SingleVariableDeclaration> parameters = new ArrayList<>();
		for (Tuple tuple : tuples) {
			if (!tuple.isParameter)
				continue;
			parameters.add(newSingleVariableDeclaration(ast, copySubtree(ast, tuple.variableDeclaration.getName()),
					copySubtree(ast, tuple.type)));
		}
		return parameters;
	}

	static Assignment newAssignment(AST ast, Expression leftHandSide, Expression rightHandSide) {
		Assignment assignment = ast.newAssignment();
		assignment.setLeftHandSide(leftHandSide);
		assignment.setRightHandSide(rightHandSide);
		return assignment;
	}

	private static Block createConstructorBody(AST ast, List<Tuple> tuples) {
		List<Statement> statements = new ArrayList<>();

		for (Tuple tuple : tuples) {
			if (!tuple.isParameter)
				continue;
			FieldAccess fieldAccess = newFieldAccess(ast, ast.newThisExpression(),
					copySubtree(ast, tuple.variableDeclaration.getName()));
			Assignment assignment = newAssignment(ast, fieldAccess,
					copySubtree(ast, tuple.variableDeclaration.getName()));
			statements.add(ast.newExpressionStatement(assignment));
		}

		Block block = ast.newBlock();
		block.statements().addAll(statements);

		return block;
	}

	private static FieldAccess newFieldAccess(AST ast, Expression expression, SimpleName name) {
		FieldAccess fieldAccess = ast.newFieldAccess();
		fieldAccess.setExpression(expression);
		fieldAccess.setName(name);
		return fieldAccess;
	}

	private static ASTParser createParser(IMethod method) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setProject(method.getJavaProject());
		parser.setSource(method.getCompilationUnit());
		parser.setResolveBindings(true);
		return parser;
	}
}
