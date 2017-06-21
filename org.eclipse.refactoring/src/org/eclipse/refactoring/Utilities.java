package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
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
	private static final String RETURN_OUTSIDE = "-1";
	private static final String SECTION = "section";
	public static final String CONTEXT_VAR = "context";
	private static final String CONTEXT = "Context";
	private static final String ITERATIVE = "Iterative";
	private static final String DEQUE = "Deque";
	private static final String LINKED_LIST = "LinkedList";
	private static final String JAVA_UTIL_LINKED_LIST = "java.util.LinkedList";
	private static final String JAVA_UTIL_DEQUE = "java.util.Deque";
	private static final String TEMP = "temp";
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

	private static List<Statement> createPopStatements(AST ast, TypeDeclaration typeDeclaration,
			MethodDeclaration method) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newSimpleName(STACK));
		invocation.setName(ast.newSimpleName("pop"));

		SimpleName topRecord = ast.newSimpleName("topRecord");

		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(topRecord);
		fragment.setInitializer(invocation);

		VariableDeclarationStatement declarationStatement = ast.newVariableDeclarationStatement(fragment);
		declarationStatement.setType(ast.newSimpleType(copySubtree(ast, typeDeclaration.getName())));

		List<Statement> statements = new ArrayList<Statement>();
		statements.add(declarationStatement);

		List<SingleVariableDeclaration> parameters = method.parameters();
		for (SingleVariableDeclaration parameter : parameters) {
			FieldAccess fieldAccess = ast.newFieldAccess();
			fieldAccess.setExpression(copySubtree(ast, topRecord));
			fieldAccess.setName(copySubtree(ast, parameter.getName()));

			Assignment assignment = ast.newAssignment();
			assignment.setLeftHandSide(copySubtree(ast, parameter.getName()));
			assignment.setRightHandSide(fieldAccess);

			statements.add(ast.newExpressionStatement(assignment));
		}

		return statements;
	}

	public static void simplifyIfStatements(AST ast, ASTRewrite astRewrite, List<Statement> statements) {
		if (statements.size() != 1 || !(statements.get(0) instanceof IfStatement)) {
			return;
		}
		IfStatement ifStatement = (IfStatement) statements.get(0);

		Statement thenStatement = ifStatement.getThenStatement();
		if (!(thenStatement instanceof Block) && !(thenStatement instanceof ReturnStatement)) {
			Block block = ast.newBlock();
			block.statements().add(copySubtree(ast, thenStatement));
			block.statements().add(ast.newReturnStatement());

			ifStatement.setThenStatement(block);
		} else if (thenStatement instanceof Block) {
			Block block = (Block) thenStatement;
			if (!(block.statements().get(block.statements().size() - 1) instanceof ReturnStatement)) {
				block.statements().add(ast.newReturnStatement());
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

		return;
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

	public static void insertAssignmentStatementBeforeStatementWithRecursiveCall(MethodInvocation invocation) {
		Block block = getParentBlock(invocation);
	}

	public static List<MethodInvocation> getInvocations(String methodName, MethodDeclaration method) {
		MethodInvocationsCollector invocationsCollector = new MethodInvocationsCollector(methodName);
		method.accept(invocationsCollector);
		return invocationsCollector.getMethodInvocations();
	}

	public static void extractRecursiveMethodInvocationsToLocalVariables(IMethod method, ASTNode astNode,
			MethodDeclaration newMethod, AST ast, ASTRewrite astRewrite) throws JavaModelException {
		List<MethodInvocation> invocations = getInvocations(method.getElementName(), newMethod);
		System.out.println(invocations);
		for (MethodInvocation invocation : invocations) {
			if (invocation.getParent() != getParentStatement(invocation)) {
				System.out.println(invocation + " is not in immediate statement.");
				int index = getIndexOfStatementWithInvocation(invocation);
				VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
				fragment.setName(ast.newSimpleName(TEMP + index));
				fragment.setInitializer(copySubtree(ast, invocation));
				VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
				Block block = getParentBlock(invocation);
				block.statements().add(index, statement);
				invocation.getParent().setStructuralProperty(invocation.getLocationInParent(),
						ast.newSimpleName(TEMP + index));
				// See ASTResolving
			}
		}
	}

	public static List<List<Statement>> getSections(String methodName, MethodDeclaration method,
			ASTRewrite astRewrite) {
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

		MethodDeclaration newMethod = createMethodDeclaration(method, astNode, astRewrite, oldMethod);

		// List<Statement> programStackStatements =
		// createProgramStack(newMethod, astNode, typeDeclaration, astRewrite);
		// WhileStatement whileStatement = createWhileStatement(ast,
		// astRewrite);

		Block body = newMethod.getBody();
		// Block whileBody = copySubtree(ast, body);
		// whileBody.statements().addAll(0, createPopStatements(ast,
		// typeDeclaration, newMethod));
		// whileStatement.setBody(whileBody);

		// body.statements().clear();
		// body.statements().addAll(programStackStatements);
		// body.statements().add(whileStatement);
		List<Statement> statements = body.statements();
		simplifyIfStatements(ast, astRewrite, statements);

		extractRecursiveMethodInvocationsToLocalVariables(method, astNode, newMethod, ast, astRewrite);

		LocalVariableCollector visitor = new LocalVariableCollector();
		newMethod.accept(visitor);
		List<Tuple> tuples = visitor.getVariableDeclarations();

		String contextName = getContextName(method.getElementName());

		listRewrite.insertLast(createTypeDeclaration(contextName, ast, tuples), null);

		List<List<Statement>> sections = getSections(method.getElementName(), newMethod, astRewrite);
		System.out.println(sections);

		statements.clear();
		statements.addAll(createHeaderStatements(method, ast, astRewrite, newMethod, tuples, contextName, sections));

		listRewrite.insertLast(newMethod, null);

		TextEdit edit = astRewrite.rewriteAST();
		ICompilationUnit unit = method.getCompilationUnit();

		TextFileChange change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
		change.setTextType("java");
		change.setEdit(edit);

		return change;
	}

	private static List<Statement> createHeaderStatements(IMethod method, AST ast, ASTRewrite astRewrite,
			MethodDeclaration newMethod, List<Tuple> tuples, String contextName, List<List<Statement>> sections) {
		List<Statement> statements = new ArrayList<>();

		statements.add(createStackDeclarationStatement(contextName, ast));
		statements.add(createPushInvocation(contextName, ast, tuples));

		Type type = newMethod.getReturnType2();
		if (type instanceof PrimitiveType) {
			PrimitiveType primitiveType = (PrimitiveType) type;
			if (primitiveType.getPrimitiveTypeCode() != PrimitiveType.VOID) {
				VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
				fragment.setName(ast.newSimpleName(RET));
				fragment.setInitializer(ast.newNumberLiteral());

				VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
				statement.setType(copySubtree(ast, type));

				statements.add(statement);
			}
		}
		statements.add(createWhileStatement(method, ast, astRewrite, sections, tuples));

		return statements;
	}

	private static boolean inTuples(List<Tuple> tuples, String name) {
		for (Tuple tuple : tuples) {
			if (tuple.variableDeclaration.getName().getFullyQualifiedName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private static FieldAccess createFieldAccess(AST ast, String name) {
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newSimpleName(Utilities.CONTEXT_VAR));
		access.setName(ast.newSimpleName(name));
		return access;
	}

	private static void replaceSimpleNameWithContextAccess(SimpleName node, List<Tuple> tuples) {
		String name = node.getFullyQualifiedName();
		if (inTuples(tuples, name)) {
			ASTNode parent = node.getParent();
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			if (parent instanceof MethodInvocation) {
				MethodInvocation invocation = (MethodInvocation) parent;
				List<Expression> arguments = invocation.arguments();
				for (Expression expression : arguments) {
					if (expression == node) {
						int index = arguments.indexOf(expression);
						arguments.remove(expression);
						arguments.add(index, createFieldAccess(node.getAST(), name));
					}
				}
			} else {
				parent.setStructuralProperty(locationInParent, createFieldAccess(node.getAST(), name));
			}
		}
	}

	private static WhileStatement createWhileStatement(IMethod method, AST ast, ASTRewrite astRewrite,
			List<List<Statement>> sections, List<Tuple> tuples) {
		WhileStatement whileStatement = ast.newWhileStatement();
		whileStatement.setExpression(ast.newBooleanLiteral(true));

		SwitchStatement statement = ast.newSwitchStatement();
		statement.setExpression(createContextSection(ast));
		List<Statement> statements = statement.statements();

		for (int i = 0; i < sections.size(); i++) {
			List<Statement> section = sections.get(i);

			SwitchCase switchCase = ast.newSwitchCase();
			switchCase.setExpression(ast.newNumberLiteral(Integer.toString(i)));
			statements.add(switchCase);

			Block block = ast.newBlock();
			List<Statement> statements2 = block.statements();
			for (Statement statement2 : section) {
				statements2.add(copySubtree(ast, statement2));
			}

			LocalVariableReplacer replacer = new LocalVariableReplacer();
			block.accept(replacer);

			LocalVariableReplacer2 replacer2 = new LocalVariableReplacer2();
			block.accept(replacer2);

			List<SimpleName> simpleNames = replacer2.getSimpleNames();
			for (SimpleName simpleName : simpleNames) {
				replaceSimpleNameWithContextAccess(simpleName, tuples);
			}

			ReturnReplacer replacer3 = new ReturnReplacer();
			block.accept(replacer3);

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
		statements2.add(createStackPeek(method, ast));
		// statements2.add(createIfStatement(ast));
		statements2.add(statement);

		whileStatement.setBody(block);

		return whileStatement;
	}

	private static IfStatement createIfStatement(AST ast) {
		FieldAccess access = createContextSection(ast);

		InfixExpression expression = ast.newInfixExpression();
		expression.setOperator(Operator.EQUALS);
		expression.setLeftOperand(access);
		expression.setRightOperand(ast.newNumberLiteral(RETURN_OUTSIDE));

		IfStatement statement = ast.newIfStatement();
		statement.setExpression(expression);
		statement.setThenStatement(ast.newBreakStatement());
		return statement;
	}

	private static FieldAccess createContextSection(AST ast) {
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newSimpleName(CONTEXT_VAR));
		access.setName(ast.newSimpleName(SECTION));
		return access;
	}

	private static VariableDeclarationStatement createStackPeek(IMethod method, AST ast) {
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setName(ast.newSimpleName(PEEK));
		invocation.setExpression(ast.newSimpleName(STACK));

		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(CONTEXT_VAR));
		fragment.setInitializer(invocation);

		VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
		statement.setType(ast.newSimpleType(ast.newSimpleName(getContextName(method.getElementName()))));
		return statement;
	}

	private static Statement createPushInvocation(String contextName, AST ast, List<Tuple> variableDeclarations) {
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(contextName)));

		List<Expression> arguments = creation.arguments();
		for (Tuple tuple : variableDeclarations) {
			if (tuple.isParameter) {
				arguments.add(copySubtree(ast, tuple.variableDeclaration.getName()));
			} else {
				arguments.add(ast.newNumberLiteral());
			}
		}

		arguments.add(ast.newNumberLiteral());

		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newSimpleName(STACK));
		invocation.setName(ast.newSimpleName(PUSH));
		invocation.arguments().add(creation);

		return ast.newExpressionStatement(invocation);
	}

	private static Statement createStackDeclarationStatement(String contextName, AST ast) {
		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(STACK));

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newParameterizedType(ast.newSimpleType(ast.newName(LINKED_LIST))));
		fragment.setInitializer(creation);

		ParameterizedType dequeType = ast.newParameterizedType(ast.newSimpleType(ast.newName(DEQUE)));
		dequeType.typeArguments().add(ast.newSimpleType(ast.newSimpleName(contextName)));

		VariableDeclarationStatement statement = ast.newVariableDeclarationStatement(fragment);
		statement.setType(dequeType);

		return statement;
	}

	private static MethodDeclaration createMethodDeclaration(IMethod method, ASTNode astNode, ASTRewrite astRewrite,
			MethodDeclaration oldMethod) throws JavaModelException {
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

		bodyDeclarations.addAll(addFieldsFromParameters(ast, tuples));
		bodyDeclarations.add(createConstructor(ast, contextName, tuples));
		bodyDeclarations.add(createDefaultConstructor(ast, contextName));

		return typeDeclaration;
	}

	private static List<FieldDeclaration> addFieldsFromParameters(AST ast, List<Tuple> tuples) {
		List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();

		for (Tuple tuple : tuples) {
			VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
			fragment.setName(copySubtree(ast, tuple.variableDeclaration.getName()));

			FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
			fieldDeclaration.setType(copySubtree(ast, tuple.type));
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

			fields.add(fieldDeclaration);
		}

		VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
		fragment.setName(ast.newSimpleName(SECTION));

		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
		fieldDeclaration.setType(ast.newPrimitiveType(PrimitiveType.INT));
		fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		fields.add(fieldDeclaration);

		return fields;
	}

	private static <T extends ASTNode> T createCopyTarget(ASTRewrite astRewrite, T node) {
		return (T) astRewrite.createCopyTarget(node);
	}

	public static <T extends ASTNode> T copySubtree(AST target, T node) {
		return (T) ASTNode.copySubtree(target, node);
	}

	private static BodyDeclaration createDefaultConstructor(AST ast, String name) {
		Block block = ast.newBlock();

		MethodDeclaration declaration = ast.newMethodDeclaration();
		declaration.setConstructor(true);
		declaration.setName(ast.newSimpleName(name));
		declaration.setBody(block);

		return declaration;
	}

	private static BodyDeclaration createConstructor(AST ast, String name, List<Tuple> tuples) {
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		constructor.setName(ast.newSimpleName(name));
		constructor.setConstructor(true);

		Block block = ast.newBlock();

		for (Tuple tuple : tuples) {
			SingleVariableDeclaration declaration = ast.newSingleVariableDeclaration();
			declaration.setName(copySubtree(ast, tuple.variableDeclaration.getName()));
			declaration.setType(copySubtree(ast, tuple.type));

			constructor.parameters().add(declaration);

			FieldAccess fieldAccess = ast.newFieldAccess();
			fieldAccess.setExpression(ast.newThisExpression());
			fieldAccess.setName(copySubtree(ast, tuple.variableDeclaration.getName()));

			Assignment assignment = ast.newAssignment();
			assignment.setLeftHandSide(fieldAccess);
			assignment.setRightHandSide(copySubtree(ast, tuple.variableDeclaration.getName()));

			block.statements().add(ast.newExpressionStatement(assignment));
		}

		SingleVariableDeclaration declaration = ast.newSingleVariableDeclaration();
		declaration.setName(ast.newSimpleName(SECTION));
		declaration.setType(ast.newPrimitiveType(PrimitiveType.INT));

		constructor.parameters().add(declaration);

		FieldAccess fieldAccess = ast.newFieldAccess();
		fieldAccess.setExpression(ast.newThisExpression());
		fieldAccess.setName(ast.newSimpleName(SECTION));

		Assignment assignment = ast.newAssignment();
		assignment.setLeftHandSide(fieldAccess);
		assignment.setRightHandSide(ast.newSimpleName(SECTION));

		block.statements().add(ast.newExpressionStatement(assignment));

		constructor.setBody(block);

		return constructor;
	}

	private static ASTParser createParser(IMethod method) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setProject(method.getJavaProject());
		parser.setSource(method.getCompilationUnit());
		parser.setResolveBindings(true);
		return parser;
	}
}
