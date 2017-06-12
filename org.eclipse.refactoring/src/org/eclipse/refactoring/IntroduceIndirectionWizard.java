package org.eclipse.refactoring;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

public class IntroduceIndirectionWizard extends RefactoringWizard {

	public IntroduceIndirectionWizard(IntroduceIndirectionRefactoring refactoring, String pageTitle) {
		super(refactoring, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE);
		setDefaultPageTitle(pageTitle);
	}

	@Override
	protected void addUserInputPages() {
		addPage(new IntroduceIndirectionInputPage("IntroduceIndirectionInputPage"));
	}
}