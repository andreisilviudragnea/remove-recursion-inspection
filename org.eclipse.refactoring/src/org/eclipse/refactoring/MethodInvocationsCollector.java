package org.eclipse.refactoring;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class MethodInvocationsCollector extends ASTVisitor {
	private String methodName;
	private List<MethodInvocation> methodInvocations = new ArrayList<MethodInvocation>();

	public MethodInvocationsCollector(String methodName) {
		super();
		this.methodName = methodName;
	}

	@Override
	public boolean visit(MethodInvocation node) {
		if (node.getName().getFullyQualifiedName().equals(methodName)) {
			methodInvocations.add(node);
		}
		return super.visit(node);
	}

	public List<MethodInvocation> getMethodInvocations() {
		return methodInvocations;
	}
}
