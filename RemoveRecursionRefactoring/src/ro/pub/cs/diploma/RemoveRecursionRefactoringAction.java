package ro.pub.cs.diploma;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

public class RemoveRecursionRefactoringAction extends AnAction {

  @Override
  public void actionPerformed(AnActionEvent e) {
    final PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    if (file == null) {
      return;
    }
    final Caret caret = e.getData(CommonDataKeys.CARET);
    if (caret == null) {
      return;
    }
    final PsiElement psiElement = file.findElementAt(caret.getOffset());
    final PsiMethod method = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class, false);
    if (method == null) {
      return;
    }
    final Project project = e.getProject();
    assert project != null;
    WriteCommandAction.runWriteCommandAction(project, () -> {
      removeRecursion(method, project, false);
    });
  }

  static void removeRecursion(PsiMethod oldMethod, Project project, boolean replaceOriginalMethod) {
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(oldMethod, PsiClass.class, true);
    if (psiClass == null) {
      return;
    }
    final List<Variable> variables = Visitors.extractVariables(oldMethod);
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
    final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
    final String frameClassName =
      styleManager.suggestUniqueVariableName(Utilities.capitalize(oldMethod.getName()) + Constants.FRAME, psiClass, true);
    PsiMethod method;
    if (replaceOriginalMethod) {
      method = oldMethod;
    }
    else {
      method = (PsiMethod)psiClass.addAfter(oldMethod, oldMethod);
      method.setName(styleManager.suggestUniqueVariableName(oldMethod.getName() + Constants.ITERATIVE, psiClass, true));
    }
    IterativeMethodGenerator.createIterativeBody(styleManager, factory, frameClassName, method, variables);
    psiClass.addAfter(FrameClassGenerator.createFrameClass(factory, oldMethod, variables, frameClassName), oldMethod);
  }
}
