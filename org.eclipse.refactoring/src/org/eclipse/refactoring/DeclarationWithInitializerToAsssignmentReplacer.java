package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class DeclarationWithInitializerToAsssignmentReplacer extends ASTVisitor {
	private static Statement createAssignmentFromDeclarationWithInitializer(VariableDeclarationFragment fragment) {
		AST ast = fragment.getAST();
		Assignment assignment = Utilities.newAssignment(ast, Utilities.copySubtree(ast, fragment.getName()),
				Utilities.copySubtree(ast, fragment.getInitializer()));
		return ast.newExpressionStatement(assignment);
	}

	private static List<Statement> createAssignmentsFromDeclarationsWithInitializer(
			VariableDeclarationStatement statement) {
		List<Statement> statements = new ArrayList<>();
		for (VariableDeclarationFragment fragment : (List<VariableDeclarationFragment>) statement.fragments()) {
			if (fragment.getInitializer() == null)
				continue;
			statements.add(createAssignmentFromDeclarationWithInitializer(fragment));
		}
		return statements;
	}

	@Override
	public void endVisit(Block node) {
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
