package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class LocalVariableCollector extends ASTVisitor {
	public static class Tuple {
		VariableDeclaration variableDeclaration;
		Type type;
		boolean isParameter;
		
		public Tuple(VariableDeclaration variableDeclaration, Type type, boolean isParameter) {
			super();
			this.variableDeclaration = variableDeclaration;
			this.type = type;
			this.isParameter = isParameter;
		}

		@Override
		public String toString() {
			return "Pair [variableDeclaration=" + variableDeclaration + ", type=" + type + ", isParameter="
					+ isParameter + "]";
		}
	}

	private List<Tuple> variableDeclarations = new ArrayList<Tuple>();

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		variableDeclarations.add(new Tuple(node, node.getType(), true));
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		variableDeclarations.add(new Tuple(node, ((VariableDeclarationStatement) node.getParent()).getType(), false));
		return super.visit(node);
	}

	public List<Tuple> getVariableDeclarations() {
		return variableDeclarations;
	}
}
