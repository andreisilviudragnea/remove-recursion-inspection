package ro.pub.cs.diploma;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
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
//        Messages.showDialog(method.getName(), "Remove Recursion", new String[]{"OK"}, -1, null);
        final PsiClass psiClass = PsiTreeUtil.getParentOfType(method, PsiClass.class, true);
        final Project project = e.getProject();
        assert project != null;
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
        WriteCommandAction.runWriteCommandAction(project, () -> {
            assert psiClass != null;
            final List<PsiVariable> variables = Visitors.extractVariables(factory, method);
            psiClass.addAfter(IterativeMethodGenerator.createIterativeMethod(project, factory, method, variables),
                    method);
            psiClass.addAfter(ContextClassGenerator.createContextClass(factory, method, variables), method);
        });
    }
}
