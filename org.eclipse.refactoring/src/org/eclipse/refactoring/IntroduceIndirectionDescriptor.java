package org.eclipse.refactoring;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class IntroduceIndirectionDescriptor extends RefactoringDescriptor {

	public static final String REFACTORING_ID= "org.eclipse.introduce.indirection";

	private final Map fArguments;

	public IntroduceIndirectionDescriptor(String project, String description, String comment, Map arguments) {
		super(REFACTORING_ID, project, description, comment, RefactoringDescriptor.STRUCTURAL_CHANGE | RefactoringDescriptor.MULTI_CHANGE);
		fArguments= arguments;
	}

	@Override
	public Refactoring createRefactoring(RefactoringStatus status) throws CoreException {
		IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring();
		status.merge(refactoring.initialize(fArguments));
		return refactoring;
	}

	public Map getArguments() {
		return fArguments;
	}
}