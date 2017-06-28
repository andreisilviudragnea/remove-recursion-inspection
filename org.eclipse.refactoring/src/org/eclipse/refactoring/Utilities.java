package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
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
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
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
import org.eclipse.text.edits.MalformedTreeException;
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
		for (MethodInvocation invocation : getInvocations(method.getElementName(), newMethod)) {
			if (invocation.getParent() == getParentStatement(invocation)) {
				continue;
			}
			int index = getIndexOfStatementWithInvocation(invocation);
			Block block = getParentBlock(invocation);
			block.statements().add(index, ast.newExpressionStatement(copySubtree(ast, invocation)));
			invocation.getParent().setStructuralProperty(invocation.getLocationInParent(), ast.newSimpleName(RET));
			// See ASTResolving
		}
	}

	public static List<List<Statement>> getSections(String methodName, MethodDeclaration method) {
		Block block = method.getBody();
		List<List<Statement>> sections = new ArrayList<>();
		int lastIndex = 0;

		for (MethodInvocation invocation : getInvocations(methodName, method)) {
			Statement statement = getParentStatement(invocation);
			if (statement.getParent() == block) {
				int index = block.statements().indexOf(statement);
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

	public static Change createContextClass(IMethod method) throws MalformedTreeException, CoreException {
		IType declaringType = method.getDeclaringType();
		ASTParser parser = createParser(method);
		ASTNode astNode = parser.createAST(null);
		AST ast = astNode.getAST();
		ASTRewrite astRewrite = ASTRewrite.create(ast);
		ImportRewrite importRewrite = ImportRewrite.create((CompilationUnit) astNode, true);

		MethodDeclaration oldMethod = (MethodDeclaration) NodeFinder.perform(astNode, method.getSourceRange());

		ASTNode declaringTypeNode = NodeFinder.perform(astNode, declaringType.getSourceRange());

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

		listRewrite.insertLast(createTypeDeclaration(contextName, ast, tuples, newMethod.parameters()), null);

		List<List<Statement>> sections = getSections(methodName, newMethod);

		statements.clear();
		Type contextType = getContextType(ast, methodName);

		statements.add(createStackDeclarationStatement(contextType, ast, importRewrite));
		statements.add(createPushInvocation(contextType, ast, newMethod.parameters()));
		addRetDeclaration(ast, newMethod.getReturnType2(), statements);
		statements.add(createWhileStatement(method, ast, sections, tuples, contextType));

		listRewrite.insertLast(newMethod, null);

		TextEdit edit = astRewrite.rewriteAST();
		edit.addChild(importRewrite.rewriteImports(null));
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
		ClassInstanceCreation creation = newClassInstanceCreation(ast, copySubtree(ast, contextType),
				arguments.stream().map(argument -> copySubtree(ast, argument)).collect(Collectors.toList()));
		MethodInvocation invocation = newMethodInvocation(ast, ast.newSimpleName(STACK), ast.newSimpleName(PUSH),
				creation);
		return ast.newExpressionStatement(invocation);
	}

	private static List<Statement> createInvocationStatements(AST ast, Type contextType, List<Tuple> tuples,
			MethodInvocation invocation) {
		List<Statement> statements = new ArrayList<>();
		statements.add(createContextIncrementStatement(ast));
		statements.add(createStackPushNewContextStatement(ast, contextType, invocation.arguments()));
		statements.add(ast.newBreakStatement());
		return statements;
	}

	private static void replaceInvocationWithStatements(AST ast, Type contextType, List<Tuple> tuples,
			MethodInvocation invocation) {
		List<Statement> statements = createInvocationStatements(ast, contextType, tuples, invocation);
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

			Block block = newBlock(ast, section.stream().map(s -> copySubtree(ast, s)).collect(Collectors.toList()));

			LocalVariableReplacer2 replacer2 = new LocalVariableReplacer2();
			block.accept(replacer2);
			for (SimpleName simpleName : replacer2.getSimpleNames()) {
				replaceSimpleNameWithContextAccess(simpleName, tuples);
			}

			ReturnReplacer replacer3 = new ReturnReplacer();
			block.accept(replacer3);

			MethodInvocationsCollector collector = new MethodInvocationsCollector(method.getElementName());
			block.accept(collector);
			for (MethodInvocation invocation : collector.getMethodInvocations()) {
				replaceInvocationWithStatements(ast, contextType, tuples, invocation);
			}

			List<Statement> statements2 = block.statements();
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
		MethodInvocation invocation = newMethodInvocation(ast, ast.newSimpleName(STACK), ast.newSimpleName(PEEK));
		VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast, ast.newSimpleName(CONTEXT_VAR),
				invocation);
		VariableDeclarationStatement statement = newVariableDeclarationStatement(ast, fragment,
				copySubtree(ast, contextType));
		return statement;
	}

	static MethodInvocation newMethodInvocation(AST ast, Expression expression, SimpleName name,
			Expression... arguments) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(expression);
		invocation.setName(name);
		((List<Expression>) invocation.arguments()).addAll(Arrays.asList(arguments));
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

	private static Statement createPushInvocation(Type contextType, AST ast,
			List<SingleVariableDeclaration> parameters) {
		ClassInstanceCreation creation = newClassInstanceCreation(ast, copySubtree(ast, contextType),
				createContextArgs(ast, parameters));
		MethodInvocation invocation = newMethodInvocation(ast, ast.newSimpleName(STACK), ast.newSimpleName(PUSH),
				creation);
		return ast.newExpressionStatement(invocation);
	}

	private static Statement createStackDeclarationStatement(Type contextType, AST ast, ImportRewrite importRewrite) {
		ClassInstanceCreation creation = newClassInstanceCreation(ast, ast.newParameterizedType(
				ast.newSimpleType(ast.newName(importRewrite.addImport(JAVA_UTIL_LINKED_LIST)))), null);
		VariableDeclarationFragment fragment = newVariableDeclarationFragment(ast, ast.newSimpleName(STACK), creation);

		ParameterizedType dequeType = ast
				.newParameterizedType(ast.newSimpleType(ast.newName(importRewrite.addImport(JAVA_UTIL_DEQUE))));
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

	private static TypeDeclaration createTypeDeclaration(String contextName, AST ast, List<Tuple> tuples,
			List<SingleVariableDeclaration> parameters) throws JavaModelException {
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(contextName));

		List<Modifier> modifiers = typeDeclaration.modifiers();
		modifiers.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		modifiers.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		bodyDeclarations.addAll(createFields(ast, tuples));
		bodyDeclarations.add(createConstructor(ast, contextName, parameters));

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

	@SuppressWarnings("unchecked")
	private static Block newBlock(AST ast, List<Statement> statements) {
		Block block = ast.newBlock();
		((List<Statement>) block.statements()).addAll(statements);
		return block;
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

	private static List<SingleVariableDeclaration> createConstructorParameters(AST ast,
			List<SingleVariableDeclaration> parameters) {
		return parameters.stream().map(parameter -> copySubtree(ast, parameter)).collect(Collectors.toList());
	}

	private static Block createConstructorBody(AST ast, List<SingleVariableDeclaration> parameters) {
		return newBlock(ast, parameters.stream().map(parameter -> {
			SimpleName name = parameter.getName();
			FieldAccess fieldAccess = newFieldAccess(ast, ast.newThisExpression(), copySubtree(ast, name));
			Assignment assignment = newAssignment(ast, fieldAccess, copySubtree(ast, name));
			return ast.newExpressionStatement(assignment);
		}).collect(Collectors.toList()));
	}

	private static BodyDeclaration createConstructor(AST ast, String name, List<SingleVariableDeclaration> parameters) {
		return newConstructor(ast, ast.newSimpleName(name), createConstructorParameters(ast, parameters),
				createConstructorBody(ast, parameters));
	}

	static Assignment newAssignment(AST ast, Expression leftHandSide, Expression rightHandSide) {
		Assignment assignment = ast.newAssignment();
		assignment.setLeftHandSide(leftHandSide);
		assignment.setRightHandSide(rightHandSide);
		return assignment;
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
