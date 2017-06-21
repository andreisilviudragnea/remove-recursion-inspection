package org.eclipse.refactoring;

import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.refactoring.LocalVariableCollector.Tuple;

public class LocalVariableReplacer2 extends ASTVisitor {
	private List<Tuple> tuples;

	public LocalVariableReplacer2(List<Tuple> tuples) {
		super();
		this.tuples = tuples;
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

	@Override
	public boolean visit(SimpleName node) {
		// TODO Auto-generated method stub
		String name = node.getFullyQualifiedName();
		if (inTuples(tuples, name)) {
			ASTNode parent = node.getParent();
			StructuralPropertyDescriptor locationInParent = node.getLocationInParent();
			parent.setStructuralProperty(locationInParent, createFieldAccess(node.getAST(), name));
		}
		return super.visit(node);
	}

}
