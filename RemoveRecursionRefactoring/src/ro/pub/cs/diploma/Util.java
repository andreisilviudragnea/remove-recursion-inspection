package ro.pub.cs.diploma;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Util {
  private Util() {
  }

  @NotNull
  static PsiStatement statement(@NotNull final PsiElementFactory factory, @NotNull final String text) {
    return factory.createStatementFromText(text, null);
  }

  @NotNull
  public static PsiElementFactory getFactory(@NotNull final PsiElement element) {
    return JavaPsiFacade.getElementFactory(element.getProject());
  }

  @NotNull
  public static JavaCodeStyleManager getStyleManager(@NotNull final PsiElement element) {
    return JavaCodeStyleManager.getInstance(element.getProject());
  }

  @Nullable
  public static PsiMethod getContainingMethod(@NotNull final PsiElement element) {
    return PsiTreeUtil.getParentOfType(element, PsiMethod.class, true, PsiClass.class, PsiLambdaExpression.class);
  }

  @NotNull
  @Contract(pure = true)
  static String getFrameClassName(@NotNull final String methodName) {
    return Utilities.capitalize(methodName) + Constants.FRAME;
  }

  public static boolean isVoid(@NotNull final PsiType returnType) {
    return returnType instanceof PsiPrimitiveType && PsiPrimitiveType.VOID.equals(returnType);
  }
}
