package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.pub.cs.diploma.passes.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class IterativeMethodGenerator {
  @NotNull private final PsiElementFactory myFactory;
  @NotNull private final JavaCodeStyleManager myStyleManager;
  @NotNull private final PsiMethod myMethod;

  private IterativeMethodGenerator(@NotNull PsiElementFactory factory,
                                   @NotNull JavaCodeStyleManager styleManager,
                                   @NotNull PsiMethod method) {
    myFactory = factory;
    myStyleManager = styleManager;
    myMethod = method;
  }

  @NotNull
  static IterativeMethodGenerator getInstance(@NotNull final PsiElementFactory factory,
                                              @NotNull final JavaCodeStyleManager styleManager,
                                              @NotNull final PsiMethod method) {
    return new IterativeMethodGenerator(factory, styleManager, method);
  }

  @NotNull
  private PsiStatement statement(@NotNull String text) {
    return myFactory.createStatementFromText(text, null);
  }

  @NotNull
  private PsiExpression expression(@NotNull String text) {
    return myFactory.createExpressionFromText(text, null);
  }

  void createIterativeBody() {
    RenameVariablesToUniqueNames.getInstance(myMethod).apply(myMethod);

    ReplaceForEachLoopsWithIteratorForLoops.getInstance(myMethod).apply(myMethod);
    ReplaceForEachLoopsWithIndexedForLoops.getInstance(myMethod).apply(myMethod);

    ReplaceSingleStatementsWithBlockStatements.getInstance(myFactory).apply(myMethod);

    ExtractRecursiveCallsToStatements.getInstance(myMethod).apply(myMethod);

    final NameManager nameManager = NameManager.getInstance(myMethod);

    FrameClassGenerator.addFrameClass(myMethod, nameManager);

    final PsiCodeBlock incorporatedBody = incorporateBody(myMethod, nameManager);
    if (incorporatedBody == null) {
      return;
    }

    replaceIdentifierWithFrameAccess(myMethod, incorporatedBody, nameManager);
    Passes.replaceDeclarationsWithInitializersWithAssignments(myMethod, incorporatedBody, nameManager);

    final BasicBlocksGenerator2 basicBlocksGenerator = new BasicBlocksGenerator2(myMethod, nameManager);
    incorporatedBody.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator2.Pair> pairs = basicBlocksGenerator.getBlocks();

    final Ref<Boolean> atLeastOneLabeledBreak = new Ref<>(false);
    pairs.forEach(pair -> replaceReturnStatements(pair.getBlock(), nameManager, atLeastOneLabeledBreak));

    final String casesString = pairs.stream().map(pair -> "case " + pair.getId() + ":" + pair.getBlock().getText())
      .collect(Collectors.joining(""));

    incorporatedBody.replace(statement(
      (atLeastOneLabeledBreak.get() ? nameManager.getSwitchLabelName() + ":" : "") +
      "switch(" + nameManager.getFrameVarName() + "." + nameManager.getBlockFieldName() + "){" + casesString + "}"));
  }

  @Nullable
  private PsiCodeBlock incorporateBody(@NotNull final PsiMethod method,
                                       @NotNull final NameManager nameManager) {
    final PsiCodeBlock body = method.getBody();
    if (body == null) {
      return null;
    }
    final String stackVarName = nameManager.getStackVarName();
    final String frameClassName = nameManager.getFrameClassName();
    final PsiWhileStatement whileStatement = (PsiWhileStatement)statement(
      "while(!" + stackVarName + ".isEmpty()){" +
      frameClassName + " " + nameManager.getFrameVarName() + "=" + stackVarName + ".peek();" + body.getText() + "}");

    final PsiCodeBlock newBody = (PsiCodeBlock)body.replace(myFactory.createCodeBlock());

    newBody.add(myStyleManager.shortenClassReferences(statement(
      "java.util.Deque<" + frameClassName + "> " + stackVarName + " = new java.util.ArrayDeque<>();")));
    newBody
      .add(
        createPushStatement(myFactory, frameClassName, stackVarName, method.getParameterList().getParameters(), PsiNamedElement::getName));
    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return null;
    }
    final String retVarName = nameManager.getRetVarName();
    if (!Util.isVoid(returnType)) {
      newBody.add(statement(returnType.getPresentableText() + " " + retVarName + "=" + getInitialValue(returnType) + ";"));
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

  private void replaceIdentifierWithFrameAccess(@NotNull final PsiMethod method,
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

    for (final PsiVariable variable : variables) {
      for (final PsiReference reference : ReferencesSearch.search(variable, new LocalSearchScope(body))) {
        if (reference instanceof PsiReferenceExpression) {
          ((PsiReferenceExpression)reference).setQualifierExpression(expression(frameVarName));
        }
      }
    }
  }

  private void replaceReturnStatements(@NotNull final PsiCodeBlock block,
                                       @NotNull final NameManager nameManager,
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
        anchor = parentBlock.addAfter(statement(nameManager.getRetVarName() + " = " + returnValue.getText() + ";"), anchor);
      }
      anchor = parentBlock.addAfter(statement(nameManager.getStackVarName() + ".pop();"), anchor);
      final boolean inLoop = PsiTreeUtil.getParentOfType(statement, PsiLoopStatement.class, true, PsiClass.class) != null;
      atLeastOneLabeledBreak.set(atLeastOneLabeledBreak.get() || inLoop);
      parentBlock.addAfter(statement("break " + (inLoop ? nameManager.getSwitchLabelName() : "") + ";"), anchor);

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
