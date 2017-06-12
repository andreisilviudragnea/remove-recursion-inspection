package org.eclipse.refactoring;

import java.util.Map;

import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

public class IntroduceIndirectionRefactoringContribution extends RefactoringContribution {

	@Override
	public RefactoringDescriptor createDescriptor(String id, String project, String description, String comment, Map arguments, int flags) {
		return new IntroduceIndirectionDescriptor(project, description, comment, arguments);
	}

	@Override
	public Map retrieveArgumentMap(RefactoringDescriptor descriptor) {
		if (descriptor instanceof IntroduceIndirectionDescriptor)
			return ((IntroduceIndirectionDescriptor) descriptor).getArguments();
		return super.retrieveArgumentMap(descriptor);
	}
}