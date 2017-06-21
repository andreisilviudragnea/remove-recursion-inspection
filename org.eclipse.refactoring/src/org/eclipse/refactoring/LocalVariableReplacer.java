package org.eclipse.refactoring;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class LocalVariableReplacer extends ASTVisitor {
	@Override
	public boolean visit(VariableDeclarationStatement node) {
		VariableDeclarationFragment fragment = (VariableDeclarationFragment) node.fragments().get(0);
		AST ast = node.getAST();
		Assignment assignment = Utilities.newAssignment(ast, Utilities.copySubtree(ast, fragment.getName()),
				Utilities.copySubtree(ast, fragment.getInitializer()));
		ASTNode astNode = node.getParent();
		if (astNode instanceof Block) {
			Block block = (Block) astNode;
			List<Statement> statements = (List<Statement>) block.statements();
			for (Statement statement : statements) {
				if (statement == node) {
					int index = statements.indexOf(statement);
					statements.remove(index);
					statements.add(index, ast.newExpressionStatement(assignment));
				}
			}
		}
		return false;
	}

}
