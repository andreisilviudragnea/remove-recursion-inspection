package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.util.RefactoringUtil;
import com.siyeh.ig.psiutils.MethodUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class IterativeMethodGenerator {
  static void createIterativeBody(PsiMethod oldMethod, Project project, boolean replaceOriginalMethod) {
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(oldMethod, PsiClass.class, true);
    if (psiClass == null) {
      return;
    }
    final PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
    final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
    final PsiMethod method;
    if (replaceOriginalMethod) {
      method = oldMethod;
    }
    else {
      method = (PsiMethod)psiClass.addAfter(oldMethod, oldMethod);
      method.setName(styleManager.suggestUniqueVariableName(oldMethod.getName() + Constants.ITERATIVE, psiClass, true));
    }

    renameVariablesToUniqueNames(styleManager, method);

    Visitors.replaceForEachStatementsWithForStatements(method);
    Visitors.replaceForStatementsWithWhileStatements(method);
    Visitors.replaceSingleStatementsWithBlockStatements(factory, method);

    PsiCodeBlock body = method.getBody();
    if (body == null) {
      return;
    }
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return;
    }
    extractRecursiveCallsToStatements(factory, styleManager, body, returnType);

    final String frameClassName =
      styleManager.suggestUniqueVariableName(Utilities.capitalize(oldMethod.getName()) + Constants.FRAME, psiClass, true);
    final String blockFieldName = styleManager.suggestUniqueVariableName(Constants.BLOCK_FIELD_NAME, method, true);

    final PsiClass frameClass = FrameClassGenerator.createFrameClass(factory, method, frameClassName, blockFieldName);
    if (frameClass == null) {
      return;
    }
    psiClass.addAfter(frameClass, method);

    final String frameVarName = styleManager.suggestUniqueVariableName(Constants.FRAME_VAR_NAME, method, true);
    final String stackVarName = styleManager.suggestUniqueVariableName(Constants.STACK_VAR_NAME, method, true);

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

    replaceIdentifierWithFrameAccess(factory, frameVarName, stackVarName, method, body);
    replaceDeclarationsWithInitializersWithAssignments(factory, frameVarName, body);

    final String switchLabelName = styleManager.suggestUniqueVariableName(Constants.SWITCH_LABEL, method, true);

    final BasicBlocksGenerator basicBlocksGenerator =
      new BasicBlocksGenerator(factory, frameClassName, frameVarName, blockFieldName, stackVarName, returnType, retVarName);
    body.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator.Pair> blocks = basicBlocksGenerator.getBlocks();

    final Ref<Boolean> atLeastOneLabeledBreak = new Ref<>(false);
    blocks.forEach(codeBlock -> replaceReturnStatements(factory, codeBlock.getBlock(), stackVarName, retVarName, switchLabelName,
                                                        atLeastOneLabeledBreak));

    final String casesString = blocks.stream()
      .map(section -> "case " + section.getId() + ": " + section.getBlock().getText())
      .collect(Collectors.joining(""));

    body.replace(factory.createStatementFromText(
      (atLeastOneLabeledBreak.get() ? switchLabelName + ": " : "") +
      "switch (" + frameVarName + "." + blockFieldName + ") {" + casesString + "}", null));
  }

  /**
   * Rename all the variables (parameters and local variables) to unique names at method level (if necessary),
   * in order to avoid name clashes when generating the Frame class.
   */
  private static void renameVariablesToUniqueNames(JavaCodeStyleManager styleManager, PsiMethod method) {
    final Map<String, Map<String, List<PsiVariable>>> names = new LinkedHashMap<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitVariable(PsiVariable variable) {
        super.visitVariable(variable);
        final String name = variable.getName();
        if (name == null) {
          return;
        }
        if (!names.containsKey(name)) {
          names.put(name, new LinkedHashMap<>());
        }
        final Map<String, List<PsiVariable>> typesMap = names.get(name);
        final String typeText = variable.getType().getCanonicalText();
        if (!typesMap.containsKey(typeText)) {
          typesMap.put(typeText, new ArrayList<>());
        }
        final List<PsiVariable> variables = typesMap.get(typeText);
        variables.add(variable);
      }
    });
    for (Map.Entry<String, Map<String, List<PsiVariable>>> entry : names.entrySet()) {
      final Map<String, List<PsiVariable>> typesMap = entry.getValue();
      if (typesMap.size() <= 1) {
        continue;
      }
      boolean first = true;
      for (List<PsiVariable> variables : typesMap.values()) {
        if (first) {
          first = false;
          continue;
        }
        final String oldName = entry.getKey();
        final String newName = styleManager.suggestUniqueVariableName(oldName, method, true);
        for (PsiVariable variable : variables) {
          RefactoringUtil.renameVariableReferences(variable, newName, new LocalSearchScope(method));
          variable.setName(newName);
        }
      }
    }
  }

  private static boolean isNotVoid(PsiType returnType) {
    return !(returnType instanceof PsiPrimitiveType) || !(PsiPrimitiveType.VOID.equals(returnType));
  }

  private static void replaceIdentifierWithFrameAccess(@NotNull final PsiElementFactory factory,
                                                       @NotNull final String frameVarName,
                                                       @NotNull final String stackVarName,
                                                       @NotNull final PsiMethod method,
                                                       @NotNull final PsiCodeBlock body) {
    List<PsiVariable> variables = new ArrayList<>();
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

    for (PsiVariable variable : variables) {
      for (PsiReference reference : ReferencesSearch.search(variable, new LocalSearchScope(body))) {
        if (reference instanceof PsiReferenceExpression) {
          PsiReferenceExpression expression = (PsiReferenceExpression)reference;
          expression.setQualifierExpression(factory.createExpressionFromText(frameVarName, null));
        }
      }
    }
  }

  private static void replaceReturnStatements(@NotNull final PsiElementFactory factory,
                                              @NotNull final PsiCodeBlock block,
                                              @NotNull final String stackVarName,
                                              @NotNull final String retVarName,
                                              @NotNull final String switchLabelName,
                                              @NotNull final Ref<Boolean> atLeastOneLabeledBreak) {
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
  static <T extends PsiElement> PsiStatement createPushStatement(@NotNull final PsiElementFactory factory,
                                                                 @NotNull final String frameClassName,
                                                                 @NotNull final String stackVarName,
                                                                 @NotNull final T[] arguments,
                                                                 @NotNull Function<T, String> function) {
    final String argumentsString = Arrays.stream(arguments).map(function).collect(Collectors.joining(","));
    return factory.createStatementFromText(stackVarName + ".push(new " + frameClassName + "(" + argumentsString + "));", null);
  }
}
