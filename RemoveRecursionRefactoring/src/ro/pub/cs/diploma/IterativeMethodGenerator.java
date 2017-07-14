package ro.pub.cs.diploma;

import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.psiutils.MethodUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class IterativeMethodGenerator {
  static void createIterativeBody(@NotNull final JavaCodeStyleManager styleManager,
                                  @NotNull final PsiElementFactory factory,
                                  @NotNull final String frameClassName,
                                  @NotNull final PsiMethod method,
                                  @NotNull final List<Variable> variables) {
    final String blockFieldName = styleManager.suggestUniqueVariableName(Constants.BLOCK_FIELD_NAME, method, true);
    variables.add(new Variable(blockFieldName, PsiType.INT.getPresentableText()));

    PsiCodeBlock body = method.getBody();
    if (body == null) {
      return;
    }

    final String frameVarName = styleManager.suggestUniqueVariableName(Constants.FRAME_VAR_NAME, body, true);
    final String stackVarName = styleManager.suggestUniqueVariableName(Constants.STACK_VAR_NAME, body, true);

    PsiWhileStatement whileStatement = (PsiWhileStatement)factory.createStatementFromText(
      "while (true) {" +
      frameClassName + " " + frameVarName + " = " + stackVarName + ".get(" + stackVarName + ".size() - 1);" +
      body.getText() +
      "}", null);

    body = (PsiCodeBlock)body.replace(factory.createCodeBlock());

    body.add(styleManager.shortenClassReferences(factory.createStatementFromText(
      "java.util.List<" + frameClassName + "> " + stackVarName + " = new java.util.ArrayList<>();", null)));
    body
      .add(createAddStatement(factory, frameClassName, stackVarName, method.getParameterList().getParameters(), PsiNamedElement::getName));
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return;
    }
    final String retVarName = styleManager.suggestUniqueVariableName(Constants.RET_VAR_NAME, body, true);
    if (!(returnType instanceof PsiPrimitiveType) || !(PsiPrimitiveType.VOID.equals(returnType))) {
      body.add(factory.createStatementFromText(
        returnType.getPresentableText() + " " + retVarName + " = " + getInitialValue(returnType) + ";", null));
    }

    whileStatement = (PsiWhileStatement)body.add(whileStatement);

    final PsiBlockStatement whileStatementBody = (PsiBlockStatement)whileStatement.getBody();
    if (whileStatementBody == null) {
      return;
    }
    final PsiBlockStatement lastBodyStatement = (PsiBlockStatement)whileStatementBody.getCodeBlock().getLastBodyElement();
    if (lastBodyStatement == null) {
      return;
    }
    body = lastBodyStatement.getCodeBlock();

    Visitors.replaceForEachStatementsWithForStatements(body);
    Visitors.replaceForStatementsWithWhileStatements(body);
    Visitors.replaceSingleStatementsWithBlockStatements(factory, body);

    extractRecursiveCallsToStatements(factory, styleManager, body, returnType);

    body.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        super.visitLocalVariable(variable);
        variables.add(new Variable(variable.getName(), variable.getType().getPresentableText()));
      }
    });

    replaceIdentifierWithFrameAccess(factory, frameVarName, variables, body);
    replaceDeclarationsWithInitializersWithAssignments(factory, frameVarName, body);

    final BasicBlocksGenerator basicBlocksGenerator =
      new BasicBlocksGenerator(factory, frameClassName, frameVarName, blockFieldName, stackVarName, returnType, retVarName);
    body.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator.Pair> blocks = basicBlocksGenerator.getBlocks();

    blocks.forEach(codeBlock -> replaceReturnStatements(factory, codeBlock.getBlock(), stackVarName, retVarName));

    final String casesString = blocks.stream()
      .map(section -> "case " + section.getId() + ": " + section.getBlock().getText())
      .collect(Collectors.joining(""));

    body.replace(factory.createStatementFromText(
      "switch (" + frameVarName + "." + blockFieldName + ") {" + casesString + "}", null));
  }

  // TODO: FIX BUG
  private static void replaceIdentifierWithFrameAccess(@NotNull final PsiElementFactory factory,
                                                       @NotNull final String frameVarName,
                                                       @NotNull final List<Variable> variables,
                                                       @NotNull final PsiCodeBlock block) {
    for (final PsiReferenceExpression expression : Visitors.extractReferenceExpressions(block)) {
      final PsiElement element = expression.resolve();
      if (element instanceof PsiLocalVariable || element instanceof PsiParameter) {
        final PsiNamedElement namedElement = (PsiNamedElement)element;
        if (variables.stream().anyMatch(variable -> variable.getName().equals(namedElement.getName()))) {
          expression.setQualifierExpression(factory.createExpressionFromText(frameVarName, null));
        }
      }
    }
  }

  private static void replaceReturnStatements(@NotNull final PsiElementFactory factory,
                                              @NotNull final PsiCodeBlock block,
                                              @NotNull final String stackVarName,
                                              @NotNull final String retVarName) {
    for (final PsiReturnStatement statement : Visitors.extractReturnStatements(block)) {
      final PsiExpression returnValue = statement.getReturnValue();
      final boolean hasExpression = returnValue != null;
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock.class, true);
      if (parentBlock == null) {
        continue;
      }
      PsiElement anchor = statement;
      if (hasExpression) {
        anchor = parentBlock.addAfter(factory.createStatementFromText(
          retVarName + " = " + returnValue.getText() + ";", null), anchor);
      }
      anchor = parentBlock.addAfter(factory.createStatementFromText(
        "if (" + stackVarName + ".size() == 1)\n" + "return " + (hasExpression ? retVarName : "") + ";\n", null), anchor);
      anchor = parentBlock.addAfter(factory.createStatementFromText(
        stackVarName + ".remove(" + stackVarName + ".size() - 1);", null), anchor);
      parentBlock.addAfter(factory.createStatementFromText("break;", null), anchor);

      statement.delete();
    }
  }

  private static void replaceDeclarationsWithInitializersWithAssignments(@NotNull final PsiElementFactory factory,
                                                                         @NotNull final String frameVarName,
                                                                         @NotNull final PsiCodeBlock block) {
    for (final PsiDeclarationStatement statement : Visitors.extractDeclarationStatements(block)) {
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock.class, true);
      if (parentBlock == null) {
        continue;
      }
      PsiElement anchor = statement;
      for (final PsiElement element : statement.getDeclaredElements()) {
        if (!(element instanceof PsiLocalVariable)) {
          continue;
        }
        final PsiLocalVariable variable = (PsiLocalVariable)element;
        final PsiExpression initializer = variable.getInitializer();
        if (initializer == null) {
          continue;
        }
        anchor = parentBlock.addAfter(factory.createStatementFromText(
          frameVarName + "." + variable.getName() + " = " + initializer.getText() + ";", null), anchor);
      }
      statement.delete();
    }
  }

  @NotNull
  private static String getInitialValue(PsiType type) {
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

  @Nullable
  static PsiMethod isRecursiveMethodCall(@NotNull final PsiMethodCallExpression expression) {
    final PsiMethod containingMethod =
      PsiTreeUtil.getParentOfType(expression, PsiMethod.class, true, PsiClass.class, PsiLambdaExpression.class);
    if (containingMethod == null) {
      return null;
    }
    final PsiMethod resolvedMethod = expression.resolveMethod();
    if (!containingMethod.equals(resolvedMethod)) {
      return null;
    }
    final PsiExpression qualifier = ParenthesesUtils.stripParentheses(expression.getMethodExpression().getQualifierExpression());
    if (qualifier != null && !(qualifier instanceof PsiThisExpression) && MethodUtils.isOverridden(containingMethod)) {
      return null;
    }
    return containingMethod;
  }

  private static void extractRecursiveCallsToStatements(@NotNull final PsiElementFactory factory,
                                                        @NotNull final JavaCodeStyleManager styleManager,
                                                        @NotNull final PsiCodeBlock block,
                                                        @NotNull final PsiType returnType) {
    for (final PsiMethodCallExpression call : Visitors.extractRecursiveCalls(block)) {
      final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
      if (parentStatement == call.getParent() && parentStatement instanceof PsiExpressionStatement) {
        continue;
      }
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
      if (parentBlock == null) {
        continue;
      }
      final String temp = styleManager.suggestUniqueVariableName(Constants.TEMP, block, true);
      parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
      call.replace(factory.createExpressionFromText(temp, null));
    }
  }

  @NotNull
  static <T extends PsiElement> PsiStatement createAddStatement(@NotNull final PsiElementFactory factory,
                                                                @NotNull final String frameClassName,
                                                                @NotNull final String stackVarName,
                                                                @NotNull final T[] arguments,
                                                                @NotNull Function<T, String> function) {
    final String argumentsString = Arrays.stream(arguments).map(function).collect(Collectors.joining(","));
    return factory.createStatementFromText(stackVarName + ".add(new " + frameClassName + "(" + argumentsString + "));", null);
  }
}
