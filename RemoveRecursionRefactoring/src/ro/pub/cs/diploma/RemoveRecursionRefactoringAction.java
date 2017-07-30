package ro.pub.cs.diploma;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

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
    if (project == null) {
      return;
    }
    WriteCommandAction.runWriteCommandAction(project, () -> IterativeMethodGenerator.createIterativeBody(method, false));
  }
}
