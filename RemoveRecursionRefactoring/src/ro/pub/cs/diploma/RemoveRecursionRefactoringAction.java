package ro.pub.cs.diploma;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

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

  static void removeRecursion(PsiMethod method, Project project, boolean replaceOriginalMethod) {
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class, true);
    if (psiClass == null) {
      return;
    }
    final List<Variable> variables = Visitors.extractVariables(method);
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
    final @Nullable PsiCodeBlock body = IterativeMethodGenerator.createIterativeBody(project, factory, method, variables);
    if (body == null) {
      return;
    }
    if (replaceOriginalMethod) {
      final PsiCodeBlock oldBody = method.getBody();
      if (oldBody == null) {
        return;
      }
      oldBody.replace(body);
    }
    else {
      psiClass.addAfter(IterativeMethodGenerator.createIterativeMethod(factory, method, body), method);
    }
    psiClass.addAfter(ContextClassGenerator.createContextClass(factory, method, variables), method);
  }
}
