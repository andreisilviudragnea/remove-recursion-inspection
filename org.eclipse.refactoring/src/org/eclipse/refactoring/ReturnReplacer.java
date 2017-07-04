package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

public class ReturnReplacer extends ASTVisitor {
	private static final String POP = "pop";
	private static final String SIZE = "size";

	private static Statement createRetAssignmentStatement(AST ast, ReturnStatement node) {
		Assignment assignment = Utilities.newAssignment(ast, ast.newSimpleName(Utilities.RET),
				Utilities.copySubtree(ast, node.getExpression()));
		return ast.newExpressionStatement(assignment);
	}

	private static Statement createIfStatement(AST ast, boolean hasExpression) {
		MethodInvocation stackSize = Utilities.newMethodInvocation(ast, ast.newSimpleName(Utilities.STACK),
				ast.newSimpleName(SIZE));

		InfixExpression expression = ast.newInfixExpression();
		expression.setLeftOperand(stackSize);
		expression.setOperator(Operator.EQUALS);
		expression.setRightOperand(ast.newNumberLiteral("1"));

		MethodInvocation stackPop = Utilities.newMethodInvocation(ast, ast.newSimpleName(Utilities.STACK),
				ast.newSimpleName(POP));

		ReturnStatement returnStatement = ast.newReturnStatement();
		if (hasExpression) {
			returnStatement.setExpression(ast.newSimpleName(Utilities.RET));
		}

		IfStatement statement = ast.newIfStatement();
		statement.setExpression(expression);
		statement.setThenStatement(returnStatement);
		statement.setElseStatement(ast.newExpressionStatement(stackPop));

		return statement;
	}

	private static List<Statement> createStatements(AST ast, ReturnStatement node) {
		List<Statement> statements = new ArrayList<>();
		boolean hasExpression = node.getExpression() != null;
		if (hasExpression) {
			statements.add(createRetAssignmentStatement(ast, node));
		}
		statements.add(createIfStatement(ast, hasExpression));
		statements.add(ast.newBreakStatement());
		return statements;
	}

	@Override
	public boolean visit(ReturnStatement node) {
		ASTNode parent = node.getParent();
		if (parent instanceof Block) {
			Block block = (Block) parent;
			List<Statement> statements = block.statements();
			AST ast = node.getAST();
			for (Statement statement : statements) {
				if (statement == node) {
					int index = statements.indexOf(statement);
					statements.remove(statement);
					statements.addAll(index, createStatements(ast, node));
				}
			}
		}
		return false;
	}

}