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
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
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
	private static final String PEEK = "peek";
	private static final String STACK = "stack";

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
		simplifyIfStatements(ast, astRewrite, body.statements());

		extractRecursiveMethodInvocationsToLocalVariables(method, astNode, newMethod, ast, astRewrite);

		LocalVariableCollector visitor = new LocalVariableCollector();
		newMethod.accept(visitor);
		List<Tuple> tuples = visitor.getVariableDeclarations();

		TypeDeclaration typeDeclaration = createTypeDeclaration(method, astNode, astRewrite, tuples);
		listRewrite.insertLast(typeDeclaration, null);

		List<List<Statement>> sections = getSections(method.getElementName(), newMethod, astRewrite);
		System.out.println(sections);

		body.statements().add(0, createStackDeclarationStatement(typeDeclaration, ast));
		body.statements().add(1, createPushInvocation(typeDeclaration, ast, tuples));
		body.statements().add(2, createWhileStatement(method, ast, astRewrite, sections, tuples));

		listRewrite.insertLast(newMethod, null);

		TextEdit edit = astRewrite.rewriteAST();
		ICompilationUnit unit = method.getCompilationUnit();

		TextFileChange change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
		change.setTextType("java");
		change.setEdit(edit);

		return change;
	}

	private static WhileStatement createWhileStatement(IMethod method, AST ast, ASTRewrite astRewrite,
			List<List<Statement>> sections, List<Tuple> tuples) {
		WhileStatement whileStatement = ast.newWhileStatement();
		whileStatement.setExpression(ast.newBooleanLiteral(true));

		SwitchStatement statement = ast.newSwitchStatement();
		statement.setExpression(createContextSection(ast));
		List statements = statement.statements();

		for (int i = 0; i < sections.size(); i++) {
			List<Statement> section = sections.get(i);

			SwitchCase switchCase = ast.newSwitchCase();
			switchCase.setExpression(ast.newNumberLiteral(Integer.toString(i)));
			statements.add(switchCase);

			Block block = ast.newBlock();
			for (Statement statement2 : section) {
				block.statements().add(copySubtree(ast, statement2));
			}
			block.statements().add(ast.newBreakStatement());
			
			LocalVariableReplacer replacer = new LocalVariableReplacer();
			block.accept(replacer);
			
			LocalVariableReplacer2 replacer2 = new LocalVariableReplacer2(tuples);
			block.accept(replacer2);

			statements.add(block);
		}

		SwitchCase defaultCase = ast.newSwitchCase();
		defaultCase.setExpression(null);
		statements.add(defaultCase);

		statements.add(ast.newBreakStatement());

		Block block = ast.newBlock();
		List<Statement> statements2 = block.statements();
		statements2.add(createStackPeek(method, ast));
		statements2.add(createIfStatement(ast));
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

	private static Statement createPushInvocation(TypeDeclaration typeDeclaration, AST ast,
			List<Tuple> variableDeclarations) {
		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(copySubtree(ast, typeDeclaration.getName())));

		List<Expression> arguments = creation.arguments();
		for (Tuple tuple : variableDeclarations) {
			if (tuple.isParameter) {
				arguments.add(copySubtree(ast, tuple.variableDeclaration.getName()));
			} else {
				arguments.add(ast.newNumberLiteral("0"));
			}
		}

		arguments.add(ast.newNumberLiteral("0"));

		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newSimpleName(STACK));
		invocation.setName(ast.newSimpleName("push"));
		invocation.arguments().add(creation);

		return ast.newExpressionStatement(invocation);
	}

	private static VariableDeclarationStatement createStackDeclarationStatement(TypeDeclaration typeDeclaration,
			AST ast) {
		VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(ast.newSimpleName(STACK));

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newParameterizedType(ast.newSimpleType(ast.newName(LINKED_LIST))));
		variableDeclarationFragment.setInitializer(creation);

		ParameterizedType dequeType = ast.newParameterizedType(ast.newSimpleType(ast.newName(DEQUE)));
		dequeType.typeArguments().add(ast.newSimpleType(copySubtree(ast, typeDeclaration.getName())));

		VariableDeclarationStatement variableDeclarationStatement = ast
				.newVariableDeclarationStatement(variableDeclarationFragment);
		variableDeclarationStatement.setType(dequeType);
		return variableDeclarationStatement;
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

	private static TypeDeclaration createTypeDeclaration(IMethod method, ASTNode astNode, ASTRewrite astRewrite,
			List<Tuple> variableDeclarations) throws JavaModelException {
		AST ast = astNode.getAST();
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(getContextName(method.getElementName())));

		List<Modifier> modifiers = typeDeclaration.modifiers();
		modifiers.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		modifiers.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		List<FieldDeclaration> fieldDeclarations = addFieldsFromParameters(ast, typeDeclaration, variableDeclarations,
				astRewrite);
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		bodyDeclarations.addAll(fieldDeclarations);

		MethodDeclaration constructor = createConstructor(ast, typeDeclaration, variableDeclarations, astRewrite);
		bodyDeclarations.add(constructor);

		return typeDeclaration;
	}

	private static List<FieldDeclaration> addFieldsFromParameters(AST ast, TypeDeclaration typeDeclaration,
			List<Tuple> variableDeclarations, ASTRewrite astRewrite) {
		List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();

		for (Tuple variableDeclaration : variableDeclarations) {
			VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
			fragment.setName(copySubtree(ast, variableDeclaration.variableDeclaration.getName()));

			FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
			fieldDeclaration.setType(copySubtree(ast, variableDeclaration.type));
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

	private static MethodDeclaration createConstructor(AST ast, TypeDeclaration typeDeclaration,
			List<Tuple> variableDeclarations, ASTRewrite astRewrite) {
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		constructor.setName(copySubtree(ast, typeDeclaration.getName()));
		constructor.setConstructor(true);

		Block block = ast.newBlock();

		for (Tuple variableDeclaration : variableDeclarations) {
			SingleVariableDeclaration declaration = ast.newSingleVariableDeclaration();
			declaration.setName(copySubtree(ast, variableDeclaration.variableDeclaration.getName()));
			declaration.setType(copySubtree(ast, variableDeclaration.type));

			constructor.parameters().add(declaration);

			FieldAccess fieldAccess = ast.newFieldAccess();
			fieldAccess.setExpression(ast.newThisExpression());
			fieldAccess.setName(copySubtree(ast, variableDeclaration.variableDeclaration.getName()));

			Assignment assignment = ast.newAssignment();
			assignment.setLeftHandSide(fieldAccess);
			assignment.setRightHandSide(copySubtree(ast, variableDeclaration.variableDeclaration.getName()));

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
