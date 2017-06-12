package org.eclipse.refactoring;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;

public class IntroduceIndirectionAction implements IWorkbenchWindowActionDelegate {

	private IMethod fMethod = null;

	private IWorkbenchWindow fWindow = null;

	public void dispose() {
		// Do nothing
	}

	public void init(IWorkbenchWindow window) {
		fWindow = window;
	}

	public void run(IAction action) {
		if (fMethod != null && fWindow != null) {
			IntroduceIndirectionRefactoring refactoring = new IntroduceIndirectionRefactoring();
			refactoring.setMethod(fMethod);
			run(new IntroduceIndirectionWizard(refactoring, "Introduce Indirection"), fWindow.getShell(),
					"Introduce Indirection");
		}
	}

	public void run(RefactoringWizard wizard, Shell parent, String dialogTitle) {
		try {
			RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(wizard);
			operation.run(parent, dialogTitle);
		} catch (InterruptedException exception) {
			// Do nothing
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fMethod = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection extended = (IStructuredSelection) selection;
			Object[] elements = extended.toArray();
			if (elements.length == 1 && elements[0] instanceof IMethod) {
				fMethod = (IMethod) elements[0];
			}
		}
		try {
			action.setEnabled(fMethod != null && fMethod.exists() && fMethod.isStructureKnown()
					&& !fMethod.isConstructor() && !fMethod.getDeclaringType().isAnnotation() && Utilities.isRecursive(fMethod));
		} catch (JavaModelException exception) {
			action.setEnabled(false);
		} catch (CoreException e) {
			action.setEnabled(false);
		}
	}
}