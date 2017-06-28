package org.eclipse.refactoring;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class DeclarationWithInitializerToAsssignmentReplacer extends ASTVisitor {
	@SuppressWarnings("unchecked")
	private static List<Statement> createAssignmentsFromDeclarationsWithInitializer(
			VariableDeclarationStatement statement) {
		return ((List<VariableDeclarationFragment>) statement.fragments()).stream()
				.filter(fragment -> fragment.getInitializer() != null).map(fragment -> {
					AST ast = fragment.getAST();
					Assignment assignment = Utilities.newAssignment(ast, Utilities.copySubtree(ast, fragment.getName()),
							Utilities.copySubtree(ast, fragment.getInitializer()));
					return ast.newExpressionStatement(assignment);
				}).collect(Collectors.toList());
	}

	@Override
	public void endVisit(Block node) {
		@SuppressWarnings("unchecked")
		List<Statement> statements = (List<Statement>) node.statements();
		for (Statement statement : statements) {
			if (!(statement instanceof VariableDeclarationStatement))
				continue;
			int index = statements.indexOf(statement);
			statement.delete();
			statements.addAll(index,
					createAssignmentsFromDeclarationsWithInitializer((VariableDeclarationStatement) statement));
		}
	}
}
