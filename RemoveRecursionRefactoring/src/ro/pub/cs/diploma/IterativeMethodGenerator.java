package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class IterativeMethodGenerator {
  static void createIterativeBody(@NotNull final PsiMethod oldMethod, final boolean replaceOriginalMethod) {
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

    Visitors.replaceForEachLoopsWithIteratorForLoops(method);
    Visitors.replaceForEachLoopsWithIndexedForLoops(method);
    Visitors.replaceSingleStatementsWithBlockStatements(method);

    extractRecursiveCallsToStatements(method);

    final NameManager nameManager = NameManager.getInstance(method);

    FrameClassGenerator.addFrameClass(method, nameManager);

    final PsiCodeBlock incorporatedBody = incorporateBody(method, nameManager);
    if (incorporatedBody == null) {
      return;
    }

    replaceIdentifierWithFrameAccess(method, incorporatedBody, nameManager);
    Passes.replaceDeclarationsWithInitializersWithAssignments(method, incorporatedBody, nameManager);

    final BasicBlocksGenerator2 basicBlocksGenerator = new BasicBlocksGenerator2(method, nameManager);
    incorporatedBody.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator2.Pair> pairs = basicBlocksGenerator.getBlocks();

    final Ref<Boolean> atLeastOneLabeledBreak = new Ref<>(false);
    pairs.forEach(pair -> replaceReturnStatements(pair.getBlock(), nameManager, atLeastOneLabeledBreak));

    final String casesString = pairs.stream().map(pair -> "case " + pair.getId() + ":" + pair.getBlock().getText())
      .collect(Collectors.joining(""));

    incorporatedBody.replace(Util.getFactory(method).createStatementFromText(
      (atLeastOneLabeledBreak.get() ? nameManager.getSwitchLabelName() + ":" : "") +
      "switch(" + nameManager.getFrameVarName() + "." + nameManager.getBlockFieldName() + "){" + casesString + "}", null));
  }

  @Nullable
  private static PsiCodeBlock incorporateBody(@NotNull final PsiMethod method,
                                              @NotNull final NameManager nameManager) {
    final PsiCodeBlock body = method.getBody();
    if (body == null) {
      return null;
    }
    final PsiElementFactory factory = Util.getFactory(method);
    final String stackVarName = nameManager.getStackVarName();
    final String frameClassName = nameManager.getFrameClassName();
    final PsiWhileStatement whileStatement = (PsiWhileStatement)factory.createStatementFromText(
      "while(!" + stackVarName + ".isEmpty()){" +
      frameClassName + " " + nameManager.getFrameVarName() + "=" + stackVarName + ".peek();" + body.getText() + "}", null);

    final PsiCodeBlock newBody = (PsiCodeBlock)body.replace(factory.createCodeBlock());

    final JavaCodeStyleManager styleManager = Util.getStyleManager(method);
    newBody.add(styleManager.shortenClassReferences(factory.createStatementFromText(
      "java.util.Deque<" + frameClassName + "> " + stackVarName + " = new java.util.ArrayDeque<>();", null)));
    newBody
      .add(createPushStatement(factory, frameClassName, stackVarName, method.getParameterList().getParameters(), PsiNamedElement::getName));
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return null;
    }
    final String retVarName = nameManager.getRetVarName();
    if (isNotVoid(returnType)) {
      newBody.add(factory.createStatementFromText(
        returnType.getPresentableText() + " " + retVarName + "=" + getInitialValue(returnType) + ";", null));
    }

    final PsiWhileStatement incorporatedWhileStatement = (PsiWhileStatement)newBody.add(whileStatement);

    if (isNotVoid(returnType)) {
      newBody.addAfter(factory.createStatementFromText("return " + retVarName + ";", null), incorporatedWhileStatement);
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

  private static boolean isNotVoid(@NotNull final PsiType returnType) {
    return !(returnType instanceof PsiPrimitiveType) || !(PsiPrimitiveType.VOID.equals(returnType));
  }

  private static void replaceIdentifierWithFrameAccess(@NotNull final PsiMethod method,
                                                       @NotNull final PsiCodeBlock body,
                                                       @NotNull final NameManager nameManager) {
    final List<PsiVariable> variables = new ArrayList<>();
    final String frameVarName = nameManager.getFrameVarName();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitParameter(PsiParameter parameter) {
        if (RecursionUtil.hasToBeSavedOnStack(parameter, method)) {
          variables.add(parameter);
        }
      }

      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        final String name = variable.getName();
        if (frameVarName.equals(name) || nameManager.getStackVarName().equals(name)) {
          return;
        }
        if (RecursionUtil.hasToBeSavedOnStack(variable, method)) {
          variables.add(variable);
        }
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
                                              @NotNull final NameManager nameManager,
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
          nameManager.getRetVarName() + " = " + returnValue.getText() + ";", null), anchor);
      }
      anchor = parentBlock.addAfter(factory.createStatementFromText(nameManager.getStackVarName() + ".pop();", null), anchor);
      final boolean inLoop = PsiTreeUtil.getParentOfType(statement, PsiLoopStatement.class, true, PsiClass.class) != null;
      atLeastOneLabeledBreak.set(atLeastOneLabeledBreak.get() || inLoop);
      parentBlock.addAfter(
        factory.createStatementFromText("break " + (inLoop ? nameManager.getSwitchLabelName() : "") + ";", null), anchor);

      statement.delete();
    }
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

  private static void extractRecursiveCallsToStatements(@NotNull final PsiMethod method) {
    final JavaCodeStyleManager styleManager = Util.getStyleManager(method);
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
                                                                 @NotNull final Function<T, String> function) {
    final String argumentsString = Arrays.stream(arguments).map(function).collect(Collectors.joining(","));
    return factory.createStatementFromText(stackVarName + ".push(new " + frameClassName + "(" + argumentsString + "));", null);
  }
}
