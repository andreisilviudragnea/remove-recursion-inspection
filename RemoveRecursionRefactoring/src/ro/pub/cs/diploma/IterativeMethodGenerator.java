package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class IterativeMethodGenerator {
  static void createIterativeBody(PsiMethod oldMethod, Project project, boolean replaceOriginalMethod) {
    final PsiMethod method;
    if (replaceOriginalMethod) {
      method = oldMethod;
    }
    else {
      final PsiClass psiClass = oldMethod.getContainingClass();
      if (psiClass == null) {
        return;
      }
      method = (PsiMethod)psiClass.addAfter(oldMethod, oldMethod);
      method.setName(oldMethod.getName() + Constants.ITERATIVE);
    }

    Passes.renameVariablesToUniqueNames(method);

    Visitors.replaceForEachStatementsWithForStatements(method);
    Visitors.replaceForStatementsWithWhileStatements(method);
    Visitors.replaceSingleStatementsWithBlockStatements(method);

    extractRecursiveCallsToStatements(method);

    final String frameClassName = Util.getFrameClassName(oldMethod.getName());
    final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
    final String blockFieldName = styleManager.suggestUniqueVariableName(Constants.BLOCK_FIELD_NAME, method, true);

    FrameClassGenerator.addFrameClass(method, frameClassName, blockFieldName);

    final String frameVarName = styleManager.suggestUniqueVariableName(Constants.FRAME_VAR_NAME, method, true);
    final String stackVarName = styleManager.suggestUniqueVariableName(Constants.STACK_VAR_NAME, method, true);

    PsiCodeBlock body = method.getBody();
    if (body == null) {
      return;
    }
    final PsiElementFactory factory = Util.getFactory(method);
    PsiWhileStatement whileStatement = (PsiWhileStatement)factory.createStatementFromText(
      "while (!" + stackVarName + ".isEmpty()) {" +
      frameClassName + " " + frameVarName + " = " + stackVarName + ".peek();" +
      body.getText() +
      "}", null);

    body = (PsiCodeBlock)body.replace(factory.createCodeBlock());

    body.add(styleManager.shortenClassReferences(factory.createStatementFromText(
      "java.util.Deque<" + frameClassName + "> " + stackVarName + " = new java.util.ArrayDeque<>();", null)));
    body
      .add(createPushStatement(factory, frameClassName, stackVarName, method.getParameterList().getParameters(), PsiNamedElement::getName));
    final String retVarName = styleManager.suggestUniqueVariableName(Constants.RET_VAR_NAME, method, true);
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return;
    }
    if (isNotVoid(returnType)) {
      body.add(factory.createStatementFromText(
        returnType.getPresentableText() + " " + retVarName + " = " + getInitialValue(returnType) + ";", null));
    }

    whileStatement = (PsiWhileStatement)body.add(whileStatement);

    if (isNotVoid(returnType)) {
      body.addAfter(factory.createStatementFromText("return " + retVarName + ";", null), whileStatement);
    }

    final PsiBlockStatement whileStatementBody = (PsiBlockStatement)whileStatement.getBody();
    if (whileStatementBody == null) {
      return;
    }
    final PsiBlockStatement lastBodyStatement = (PsiBlockStatement)whileStatementBody.getCodeBlock().getLastBodyElement();
    if (lastBodyStatement == null) {
      return;
    }
    body = lastBodyStatement.getCodeBlock();

    replaceIdentifierWithFrameAccess(frameVarName, stackVarName, method, body);
    replaceDeclarationsWithInitializersWithAssignments(frameVarName, body);

    final String switchLabelName = styleManager.suggestUniqueVariableName(Constants.SWITCH_LABEL, method, true);

    final BasicBlocksGenerator2 basicBlocksGenerator =
      new BasicBlocksGenerator2(method, frameClassName, frameVarName, blockFieldName, stackVarName, retVarName);
    body.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator2.Pair> blocks = basicBlocksGenerator.getBlocks();

    final Ref<Boolean> atLeastOneLabeledBreak = new Ref<>(false);
    blocks.forEach(
      codeBlock -> replaceReturnStatements(codeBlock.getBlock(), stackVarName, retVarName, switchLabelName, atLeastOneLabeledBreak));

    final String casesString = blocks.stream()
      .map(section -> "case " + section.getId() + ": " + section.getBlock().getText())
      .collect(Collectors.joining(""));

    body.replace(factory.createStatementFromText(
      (atLeastOneLabeledBreak.get() ? switchLabelName + ": " : "") +
      "switch (" + frameVarName + "." + blockFieldName + ") {" + casesString + "}", null));
  }

  private static boolean isNotVoid(PsiType returnType) {
    return !(returnType instanceof PsiPrimitiveType) || !(PsiPrimitiveType.VOID.equals(returnType));
  }

  private static void replaceIdentifierWithFrameAccess(@NotNull final String frameVarName,
                                                       @NotNull final String stackVarName,
                                                       @NotNull final PsiMethod method,
                                                       @NotNull final PsiCodeBlock body) {
    final List<PsiVariable> variables = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitParameter(PsiParameter parameter) {
        super.visitParameter(parameter);
        variables.add(parameter);
      }

      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        super.visitLocalVariable(variable);
        final String name = variable.getName();
        if (frameVarName.equals(name) || stackVarName.equals(name)) {
          return;
        }
        variables.add(variable);
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });

    final PsiElementFactory factory = Util.getFactory(method);
    for (final PsiVariable variable : variables) {
      for (final PsiReference reference : ReferencesSearch.search(variable, new LocalSearchScope(body))) {
        if (reference instanceof PsiReferenceExpression) {
          PsiReferenceExpression expression = (PsiReferenceExpression)reference;
          expression.setQualifierExpression(factory.createExpressionFromText(frameVarName, null));
        }
      }
    }
  }

  private static void replaceReturnStatements(@NotNull final PsiCodeBlock block,
                                              @NotNull final String stackVarName,
                                              @NotNull final String retVarName,
                                              @NotNull final String switchLabelName,
                                              @NotNull final Ref<Boolean> atLeastOneLabeledBreak) {
    final PsiElementFactory factory = Util.getFactory(block);
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
      anchor = parentBlock.addAfter(factory.createStatementFromText(stackVarName + ".pop();", null), anchor);
      final boolean inLoop = PsiTreeUtil.getParentOfType(statement, PsiLoopStatement.class, true, PsiClass.class) != null;
      atLeastOneLabeledBreak.set(atLeastOneLabeledBreak.get() || inLoop);
      parentBlock.addAfter(factory.createStatementFromText("break " + (inLoop ? switchLabelName : "") + ";", null), anchor);

      statement.delete();
    }
  }

  private static void replaceDeclarationsWithInitializersWithAssignments(@NotNull final String frameVarName,
                                                                         @NotNull final PsiCodeBlock block) {
    final PsiElementFactory factory = Util.getFactory(block);
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
        anchor = parentBlock
          .addAfter(factory.createStatementFromText(frameVarName + "." + variable.getName() + " = " + initializer.getText() + ";", null),
                    anchor);
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

  private static void extractRecursiveCallsToStatements(@NotNull final PsiMethod method) {
    final Project project = method.getProject();
    final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
    final PsiElementFactory factory = Util.getFactory(method);
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return;
    }
    for (final PsiMethodCallExpression call : Visitors.extractRecursiveCalls(method)) {
      final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
      if (parentStatement == call.getParent() && parentStatement instanceof PsiExpressionStatement) {
        continue;
      }
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
      if (parentBlock == null) {
        continue;
      }
      final String temp = styleManager.suggestUniqueVariableName(Constants.TEMP, method, true);
      parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
      call.replace(factory.createExpressionFromText(temp, null));
    }
  }

  @NotNull
  static <T extends PsiElement> PsiStatement createPushStatement(@NotNull final PsiElementFactory factory,
                                                                 @NotNull final String frameClassName,
                                                                 @NotNull final String stackVarName,
                                                                 @NotNull final T[] arguments,
                                                                 @NotNull Function<T, String> function) {
    final String argumentsString = Arrays.stream(arguments).map(function).collect(Collectors.joining(","));
    return factory.createStatementFromText(stackVarName + ".push(new " + frameClassName + "(" + argumentsString + "));", null);
  }
}
