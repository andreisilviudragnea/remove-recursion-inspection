package org.eclipse.refactoring;

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
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
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
		astRewrite.getListRewrite(declaringTypeNode, TypeDeclaration.BODY_DECLARATIONS_PROPERTY)
				.insertLast(typeDeclaration, null);
		TextEdit edit = astRewrite.rewriteAST();
		ICompilationUnit unit = method.getCompilationUnit();
		TextFileChange change = new TextFileChange(unit.getElementName(), (IFile) unit.getResource());
		change.setTextType("java");
		change.setEdit(edit);
		return change;
	}

	private static TypeDeclaration createTypeDeclaration(IMethod method, ASTNode astNode, ASTRewrite astRewrite) throws JavaModelException {
		AST ast = astNode.getAST();
		TypeDeclaration typeDeclaration = ast.newTypeDeclaration();
		typeDeclaration.setName(ast.newSimpleName(method.getElementName() + "Context"));
		List<Modifier> modifiers = typeDeclaration.modifiers();
		modifiers.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
		modifiers.add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));
		MethodDeclaration methodDeclaration = (MethodDeclaration) NodeFinder.perform(astNode, method.getSourceRange());
		List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		addFieldsFromParameters(ast, typeDeclaration, parameters, astRewrite);
		return typeDeclaration;
	}

	private static void addFieldsFromParameters(AST ast, TypeDeclaration typeDeclaration,
			List<SingleVariableDeclaration> parameters, ASTRewrite astRewrite) {
		for (SingleVariableDeclaration parameter: parameters) {
			VariableDeclarationFragment fragment = ast.newVariableDeclarationFragment();
			fragment.setName((SimpleName) astRewrite.createCopyTarget(parameter.getName()));
			
			FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(fragment);
			fieldDeclaration.setType((Type) astRewrite.createCopyTarget(parameter.getType()));
			fieldDeclaration.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			
			typeDeclaration.bodyDeclarations().add(fieldDeclaration);
		}
	}

	private static ASTParser createParser(IMethod method) {
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setProject(method.getJavaProject());
		parser.setSource(method.getCompilationUnit());
		parser.setResolveBindings(true);
		return parser;
	}
}
