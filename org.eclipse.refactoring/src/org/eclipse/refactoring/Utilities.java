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
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
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
import org.eclipse.text.edits.TextEdit;

public class Utilities {
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

	public static Change createContextClass(IMethod method) throws JavaModelException {
		IType declaringType = method.getDeclaringType();
		ASTParser parser = createParser(method);
		ASTNode astNode = parser.createAST(null);
		AST ast = astNode.getAST();
		ASTRewrite astRewrite = ASTRewrite.create(ast);
		TypeDeclaration typeDeclaration = createTypeDeclaration(method, astNode, astRewrite);
		ASTNode declaringTypeNode = NodeFinder.perform(astNode, declaringType.getSourceRange());
		ListRewrite listRewrite = astRewrite.getListRewrite(declaringTypeNode,
				TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		listRewrite.insertLast(typeDeclaration, null);
		MethodDeclaration newMethod = createMethodDeclaration(method, astNode, astRewrite);
		Block body = newMethod.getBody();
		body.statements().addAll(0, createProgramStack(newMethod, astNode, typeDeclaration, astRewrite));
		listRewrite.insertLast(newMethod, null);
		TextEdit edit = astRewrite.rewriteAST();
		ICompilationUnit unit = method.getCompilationUnit();
		TextFileChange change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
		change.setTextType("java");
		change.setEdit(edit);
		return change;
	}

	private static List<Statement> createProgramStack(MethodDeclaration method, ASTNode root,
			TypeDeclaration typeDeclaration, ASTRewrite astRewrite) {
		AST ast = root.getAST();

		VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
		variableDeclarationFragment.setName(ast.newSimpleName("programStack"));

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newParameterizedType(ast.newSimpleType(ast.newName("java.util.LinkedList"))));
		variableDeclarationFragment.setInitializer(creation);

		ParameterizedType dequeType = ast.newParameterizedType(ast.newSimpleType(ast.newName("java.util.Deque")));
		dequeType.typeArguments().add(ast.newSimpleType(copySubtree(ast, typeDeclaration.getName())));

		VariableDeclarationStatement variableDeclarationStatement = ast
				.newVariableDeclarationStatement(variableDeclarationFragment);
		variableDeclarationStatement.setType(dequeType);
		
		ClassInstanceCreation creation2 = ast.newClassInstanceCreation();
		creation2.setType(ast.newSimpleType(copySubtree(ast, typeDeclaration.getName())));
		List<SingleVariableDeclaration> parameters = method.parameters();
		for (SingleVariableDeclaration parameter: parameters) {
			creation2.arguments().add(copySubtree(ast, parameter.getName()));
		}
		
		MethodInvocation invocation = ast.newMethodInvocation();
		invocation.setExpression(ast.newSimpleName("programStack"));
		invocation.setName(ast.newSimpleName("push"));
		invocation.arguments().add(creation2);

		List<Statement> statements = new ArrayList<Statement>();
		statements.add(variableDeclarationStatement);
		statements.add(ast.newExpressionStatement(invocation));
		return statements;
	}

	private static MethodDeclaration createMethodDeclaration(IMethod method, ASTNode astNode, ASTRewrite astRewrite)
			throws JavaModelException {
		MethodDeclaration oldMethod = extractMethodDeclaration(method, astNode);
		AST ast = astNode.getAST();
		MethodDeclaration newMethod = copySubtree(ast, oldMethod);
		newMethod.setName(ast.newSimpleName(newMethod.getName().getFullyQualifiedName() + "Iteratively"));
		return newMethod;
	}

	private static MethodDeclaration extractMethodDeclaration(IMethod method, ASTNode astNode)
			throws JavaModelException {
		return (MethodDeclaration) NodeFinder.perform(astNode, method.getSourceRange());
	}

	private static TypeDeclaration createTypeDeclaration(IMethod method, ASTNode astNode, ASTRewrite astRewrite)
			throws JavaModelException {
		AST ast = astNode.getAST();
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(method.getElementName() + "Context"));

		List<Modifier> modifiers = typeDeclaration.modifiers();
		modifiers.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		modifiers.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		MethodDeclaration methodDeclaration = extractMethodDeclaration(method, astNode);
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		List<FieldDeclaration> fieldDeclarations = addFieldsFromParameters(ast, typeDeclaration, parameters,
				astRewrite);
		List<BodyDeclaration> bodyDeclarations = typeDeclaration.bodyDeclarations();
		bodyDeclarations.addAll(fieldDeclarations);

		MethodDeclaration constructor = createConstructor(ast, typeDeclaration, parameters, astRewrite);
		bodyDeclarations.add(constructor);

		return typeDeclaration;
	}

	private static List<FieldDeclaration> addFieldsFromParameters(AST ast, TypeDeclaration typeDeclaration,
			List<SingleVariableDeclaration> parameters, ASTRewrite astRewrite) {
		List<FieldDeclaration> fields = new ArrayList<FieldDeclaration>();

		for (SingleVariableDeclaration parameter : parameters) {
			VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
			fragment.setName(createCopyTarget(astRewrite, parameter.getName()));

			FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
			fieldDeclaration.setType(createCopyTarget(astRewrite, parameter.getType()));
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

			fields.add(fieldDeclaration);
		}

		return fields;
	}

	private static <T extends ASTNode> T createCopyTarget(ASTRewrite astRewrite, T node) {
		return (T) astRewrite.createCopyTarget(node);
	}
	
	private static <T extends ASTNode> T copySubtree(AST target, T node) {
		return (T) ASTNode.copySubtree(target, node);
	}

	private static MethodDeclaration createConstructor(AST ast, TypeDeclaration typeDeclaration,
			List<SingleVariableDeclaration> parameters, ASTRewrite astRewrite) {
		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		constructor.setName(copySubtree(ast, typeDeclaration.getName()));
		constructor.setConstructor(true);

		Block block = ast.newBlock();

		for (SingleVariableDeclaration parameter : parameters) {
			constructor.parameters().add(createCopyTarget(astRewrite, parameter));

			FieldAccess fieldAccess = ast.newFieldAccess();
			fieldAccess.setExpression(ast.newThisExpression());
			fieldAccess.setName(createCopyTarget(astRewrite, parameter.getName()));

			Assignment assignment = ast.newAssignment();
			assignment.setLeftHandSide(fieldAccess);
			assignment.setRightHandSide(createCopyTarget(astRewrite, parameter.getName()));

			block.statements().add(ast.newExpressionStatement(assignment));
		}

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
