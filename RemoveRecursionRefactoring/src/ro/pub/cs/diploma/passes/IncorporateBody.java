package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.pub.cs.diploma.NameManager;
import ro.pub.cs.diploma.Util;

public class IncorporateBody implements Pass<PsiMethod, PsiMethod, PsiCodeBlock> {
  @NotNull private final NameManager myNameManager;
  @NotNull private final PsiElementFactory myFactory;
  @NotNull private final JavaCodeStyleManager myStyleManager;

  private IncorporateBody(@NotNull final NameManager nameManager,
                          @NotNull final PsiElementFactory factory,
                          @NotNull final JavaCodeStyleManager styleManager) {
    myNameManager = nameManager;
    myFactory = factory;
    myStyleManager = styleManager;
  }

  @NotNull
  public static IncorporateBody getInstance(@NotNull final NameManager nameManager,
                                     @NotNull final PsiElementFactory factory,
                                     @NotNull final JavaCodeStyleManager styleManager) {
    return new IncorporateBody(nameManager, factory, styleManager);
  }

  @Override
  @NotNull
  public PsiMethod collect(PsiMethod method) {
    return method;
  }

  @NotNull
  private PsiStatement statement(@NotNull String text) {
    return myFactory.createStatementFromText(text, null);
  }

  @NotNull
  private static String getInitialValue(@NotNull final PsiType type) {
    if (PsiPrimitiveType.BYTE.equals(type)) {
      return "(byte) 0";
    }
    if (PsiPrimitiveType.SHORT.equals(type)) {
      return "(short) 0";
    }
    if (PsiPrimitiveType.INT.equals(type)) {
      return "0";
    }
    if (PsiPrimitiveType.LONG.equals(type)) {
      return "0L";
    }
    if (PsiPrimitiveType.FLOAT.equals(type)) {
      return "0.0f";
    }
    if (PsiPrimitiveType.DOUBLE.equals(type)) {
      return "0.0d";
    }
    if (PsiPrimitiveType.CHAR.equals(type)) {
      return "'\u0000'";
    }
    if (PsiPrimitiveType.BOOLEAN.equals(type)) {
      return "false";
    }
    return "null";
  }

  @Override
  @Nullable
  public PsiCodeBlock transform(PsiMethod method) {
    final PsiCodeBlock body = method.getBody();
    if (body == null) {
      return null;
    }
    final String stackVarName = myNameManager.getStackVarName();
    final String frameClassName = myNameManager.getFrameClassName();
    final PsiWhileStatement whileStatement = (PsiWhileStatement)statement(
      "while(!" + stackVarName + ".isEmpty()){" +
      frameClassName + " " + myNameManager.getFrameVarName() + "=" + stackVarName + ".peek();" + body.getText() + "}");

    final PsiCodeBlock newBody = (PsiCodeBlock)body.replace(myFactory.createCodeBlock());

    newBody.add(myStyleManager.shortenClassReferences(statement(
      "java.util.Deque<" + frameClassName + "> " + stackVarName + " = new java.util.ArrayDeque<>();")));
    newBody.add(Util.createPushStatement(myFactory, frameClassName, stackVarName,
                                         method.getParameterList().getParameters(), PsiNamedElement::getName));
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return null;
    }
    final String retVarName = myNameManager.getRetVarName();
    if (!Util.isVoid(returnType)) {
      newBody.add(myStyleManager.shortenClassReferences(statement(
        returnType.getCanonicalText() + " " + retVarName + "=" + getInitialValue(returnType) + ";")));
    }

    final PsiWhileStatement incorporatedWhileStatement = (PsiWhileStatement)newBody.add(whileStatement);

    if (!Util.isVoid(returnType)) {
      newBody.addAfter(statement("return " + retVarName + ";"), incorporatedWhileStatement);
    }

    final PsiBlockStatement whileStatementBody = (PsiBlockStatement)incorporatedWhileStatement.getBody();
    if (whileStatementBody == null) {
      return null;
    }
    final PsiBlockStatement lastBodyStatement = (PsiBlockStatement)whileStatementBody.getCodeBlock().getLastBodyElement();
    if (lastBodyStatement == null) {
      return null;
    }
    return lastBodyStatement.getCodeBlock();
  }
}
