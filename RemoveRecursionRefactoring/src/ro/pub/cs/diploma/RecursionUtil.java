package ro.pub.cs.diploma;

import com.intellij.codeInsight.daemon.impl.RecursiveCallLineMarkerProvider;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.siyeh.ig.performance.TailRecursionInspection;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecursionUtil {
  private RecursionUtil() {

  }

  /**
   * Checks if the {@code expression} is a recursive method call to {@code method}.
   *
   * @see RecursiveCallLineMarkerProvider#isRecursiveMethodCall(PsiMethodCallExpression)
   * @see TailRecursionInspection.TailRecursionVisitor#visitReturnStatement(PsiReturnStatement)
   * @see com.siyeh.ig.psiutils.RecursionUtils
   * @see com.siyeh.ig.psiutils.RecursionVisitor
   */
  public static boolean isRecursive(@NotNull final PsiMethodCallExpression expression, @NotNull final PsiMethod method) {
    final PsiReferenceExpression methodExpression = expression.getMethodExpression();
    if (!method.getName().equals(methodExpression.getReferenceName())) {
      return false;
    }
    final PsiMethod calledMethod = expression.resolveMethod();
    if (!method.equals(calledMethod)) {
      return false;
    }
    if (method.hasModifierProperty(PsiModifier.STATIC) || method.hasModifierProperty(PsiModifier.PRIVATE)) {
      return true;
    }
    final PsiExpression qualifier = ParenthesesUtils.stripParentheses(methodExpression.getQualifierExpression());
    return qualifier == null || qualifier instanceof PsiThisExpression;
  }

  /**
   * Returns true if the specified {@code element} contains at least on recursive call to the specified {@code method}.
   */
  static boolean containsRecursiveCalls(@NotNull final PsiElement element, @NotNull final PsiMethod method) {
    final Ref<Boolean> contains = new Ref<>(false);
    element.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (!isRecursive(expression, method)) {
          return;
        }
        contains.set(true);
        stopWalking();
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    return contains.get();
  }

  @NotNull
  static Set<PsiStatement> extractStatementsContainingRecursiveCalls(@NotNull final PsiCodeBlock incorporatedBody,
                                                                     @NotNull final PsiMethod method) {
    List<PsiMethodCallExpression> recursiveCalls = new ArrayList<>();
    incorporatedBody.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitMethodCallExpression(PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);
        if (RecursionUtil.isRecursive(expression, method)) {
          recursiveCalls.add(expression);
        }
      }
    });

    Set<PsiStatement> statementsContainingRecursiveCalls = new HashSet<>();
    for (final PsiMethodCallExpression call : recursiveCalls) {
      PsiElement parent = call.getParent();
      while (parent != incorporatedBody) {
        if (parent instanceof PsiStatement) {
          statementsContainingRecursiveCalls.add((PsiStatement)parent);
        }
        parent = parent.getParent();
      }
    }

    return statementsContainingRecursiveCalls;
  }

  @NotNull
  private static List<PsiElement> getElementsInScope(@NotNull final PsiParameter parameter) {
    final PsiElement parent = parameter.getParent();
    final List<PsiElement> elements = new ArrayList<>();

    if (parent instanceof PsiParameterList) {
      final PsiCodeBlock body = ((PsiMethod)parent.getParent()).getBody();
      if (body != null) {
        elements.add(body);
      }
    }
    else if (parent instanceof PsiForeachStatement) {
      final PsiStatement body = ((PsiForeachStatement)parent).getBody();
      if (body != null) {
        elements.add(body);
      }
    }

    return elements;
  }

  public static boolean hasToBeSavedOnStack(@NotNull final PsiParameter parameter, @NotNull final PsiMethod method) {
    for (PsiElement element : getElementsInScope(parameter)) {
      if (containsRecursiveCalls(element, method)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  private static List<PsiElement> getElementsInScope(@NotNull final PsiLocalVariable variable) {
    final PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement)variable.getParent();
    final PsiElement parent = declarationStatement.getParent();

    final List<PsiElement> elements = new ArrayList<>();

    // This is because the variable is actually used only after it has been initialized.
    //final PsiExpression initializer = variable.getInitializer();
    //if (initializer != null) {
    //  elements.add(initializer);
    //}

    boolean met = false;
    for (final PsiElement element : declarationStatement.getDeclaredElements()) {
      if (element instanceof PsiLocalVariable) {
        if (met) {
          elements.add(element);
        }
        if (element == variable) {
          met = true;
        }
      }
    }

    if (parent instanceof PsiForStatement) {
      PsiForStatement statement = (PsiForStatement)parent;
      final PsiExpression condition = statement.getCondition();
      if (condition != null) {
        elements.add(condition);
      }
      final PsiStatement update = statement.getUpdate();
      if (update != null) {
        elements.add(update);
      }
      final PsiStatement body = statement.getBody();
      if (body != null) {
        elements.add(body);
      }
    }
    else if (parent instanceof PsiCodeBlock) {
      final PsiCodeBlock block = (PsiCodeBlock)parent;

      met = false;
      for (PsiStatement psiStatement : block.getStatements()) {
        if (met) {
          elements.add(psiStatement);
        }
        if (psiStatement == declarationStatement) {
          met = true;
        }
      }
    }

    return elements;
  }

  public static boolean hasToBeSavedOnStack(@NotNull final PsiLocalVariable variable, @NotNull final PsiMethod method) {
    for (PsiElement element : getElementsInScope(variable)) {
      if (containsRecursiveCalls(element, method)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasToBeSavedOnStack(@NotNull final PsiDeclarationStatement statement, @NotNull final PsiMethod method) {
    for (PsiElement element : statement.getDeclaredElements()) {
      if (element instanceof PsiLocalVariable) {
        if (hasToBeSavedOnStack((PsiLocalVariable)element, method)) {
          return true;
        }
      }
    }
    return false;
  }
}
