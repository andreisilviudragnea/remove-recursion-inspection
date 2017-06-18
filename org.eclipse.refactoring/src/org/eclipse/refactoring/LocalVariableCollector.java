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
	public static class Pair {
		VariableDeclaration variableDeclaration;
		Type type;

		public Pair(VariableDeclaration variableDeclaration, Type type) {
			super();
			this.variableDeclaration = variableDeclaration;
			this.type = type;
		}

		@Override
		public String toString() {
			return "Pair [variableDeclaration=" + variableDeclaration + ", type=" + type + "]";
		}
	}

	private List<Pair> variableDeclarations = new ArrayList<Pair>();

	@Override
	public boolean visit(SingleVariableDeclaration node) {
		variableDeclarations.add(new Pair(node, node.getType()));
		return super.visit(node);
	}

	@Override
	public boolean visit(VariableDeclarationFragment node) {
		variableDeclarations.add(new Pair(node, ((VariableDeclarationStatement) node.getParent()).getType()));
		return super.visit(node);
	}

	public List<Pair> getVariableDeclarations() {
		return variableDeclarations;
	}
}
