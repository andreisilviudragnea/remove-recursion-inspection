package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.SimpleName;

public class LocalVariableReplacer2 extends ASTVisitor {
	private List<SimpleName> simpleNames = new ArrayList<>();

	@Override
	public boolean visit(SimpleName node) {
		simpleNames.add(node);
		return super.visit(node);
	}

	public List<SimpleName> getSimpleNames() {
		return simpleNames;
	}
}
