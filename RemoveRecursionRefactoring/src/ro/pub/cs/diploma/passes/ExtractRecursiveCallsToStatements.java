package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.Constants;
import ro.pub.cs.diploma.RecursionUtil;
import ro.pub.cs.diploma.Util;

import java.util.ArrayList;
import java.util.List;

public class ExtractRecursiveCallsToStatements implements Pass<PsiMethod, List<PsiMethodCallExpression>, Object> {
  @NotNull private final PsiMethod myMethod;

  private ExtractRecursiveCallsToStatements(@NotNull PsiMethod method) {
    myMethod = method;
  }

  @NotNull
  public static ExtractRecursiveCallsToStatements getInstance(@NotNull PsiMethod method) {
    return new ExtractRecursiveCallsToStatements(method);
  }

  @Override
  public List<PsiMethodCallExpression> collect(PsiMethod method) {
    final List<PsiMethodCallExpression> calls = new ArrayList<>();
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return calls;
    }
    if (Util.isVoid(returnType)) {
      return calls;
    }
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (RecursionUtil.isRecursive(expression, method)) {
          calls.add(expression);
        }
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    return calls;
  }

  @Override
  public Object transform(List<PsiMethodCallExpression> expressions) {
    final JavaCodeStyleManager styleManager = Util.getStyleManager(myMethod);
    final PsiElementFactory factory = Util.getFactory(myMethod);
    final PsiType returnType = myMethod.getReturnType();
    if (returnType == null) {
      return null;
    }
    calls:
    for (final PsiMethodCallExpression call : expressions) {
      final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
      if (parentStatement instanceof PsiDeclarationStatement) {
        for (PsiElement element : ((PsiDeclarationStatement)parentStatement).getDeclaredElements()) {
          if (element instanceof PsiLocalVariable) {
            if (((PsiLocalVariable)element).getInitializer() == call) {
              continue calls;
            }
          }
        }
      }
      if (parentStatement instanceof PsiExpressionStatement) {
        PsiExpression expression = ((PsiExpressionStatement)parentStatement).getExpression();
        if (expression instanceof PsiAssignmentExpression && ((PsiAssignmentExpression)expression).getRExpression() == call) {
          continue;
        }
      }
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
      if (parentBlock == null) {
        continue;
      }
      final String temp = styleManager.suggestUniqueVariableName(Constants.TEMP, myMethod, true);
      parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
      call.replace(factory.createExpressionFromText(temp, null));
    }
    return null;
  }
}
