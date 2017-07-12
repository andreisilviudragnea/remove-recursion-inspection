package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class IterativeMethodGenerator {

  static PsiMethod createIterativeMethod(PsiElementFactory factory, PsiMethod oldMethod, PsiCodeBlock body) {
    final PsiMethod method = factory.createMethod(oldMethod.getName() + "Iterative", oldMethod.getReturnType());

    // Copy parameters
    final PsiParameterList parameterList = method.getParameterList();
    Arrays.stream(oldMethod.getParameterList().getParameters()).forEach(parameterList::add);

    // Copy modifiers
    final PsiModifierList modifierList = method.getModifierList();
    final PsiModifierList oldMethodModifierList = oldMethod.getModifierList();
    Arrays.stream(PsiModifier.MODIFIERS)
      .forEach(modifier -> modifierList.setModifierProperty(modifier, oldMethodModifierList.hasExplicitModifier(modifier)));

    final PsiCodeBlock oldBody = method.getBody();
    if (oldBody == null) {
      return method;
    }

    oldBody.replace(body);

    return method;
  }

  @Nullable
  static PsiCodeBlock createIterativeBody(@NotNull final Project project,
                                          @NotNull final PsiElementFactory factory,
                                          @NotNull final PsiClass psiClass,
                                          @NotNull final PsiMethod oldMethod,
                                          @NotNull final List<Variable> variables) {
    final JavaCodeStyleManager styleManager = JavaCodeStyleManager.getInstance(project);
    final String oldMethodName = oldMethod.getName();
    final String frameClassName =
      styleManager.suggestUniqueVariableName(Utilities.capitalize(oldMethodName) + Constants.FRAME, psiClass, true);
    final PsiType returnType = oldMethod.getReturnType();
    if (returnType == null) {
      return null;
    }

    final String blockFieldName = styleManager.suggestUniqueVariableName(Constants.BLOCK_FIELD_NAME, oldMethod, true);
    variables.add(new Variable(blockFieldName, PsiType.INT.getPresentableText()));

    final PsiCodeBlock block = (PsiCodeBlock)oldMethod.getBody().copy();

    Visitors.replaceForEachStatementsWithForStatements(block);
    Visitors.replaceForStatementsWithWhileStatements(block);

    block.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitLocalVariable(PsiLocalVariable variable) {
        super.visitLocalVariable(variable);
        variables.add(new Variable(variable.getName(), variable.getType().getPresentableText()));
      }
    });

    final String frameVarName = styleManager.suggestUniqueVariableName(Constants.FRAME_VAR_NAME, block, true);

    Visitors.replaceSingleStatementsWithBlockStatements(factory, block);
    extractRecursiveCallsToStatements(factory, styleManager, block, oldMethodName, returnType, variables);

    replaceIdentifierWithFrameAccess(factory, frameVarName, variables, block);
    replaceDeclarationsWithInitializersWithAssignments(factory, frameVarName, block);

    final String stackVarName = styleManager.suggestUniqueVariableName(Constants.STACK_VAR_NAME, block, true);
    final String retVarName = styleManager.suggestUniqueVariableName(Constants.RET_VAR_NAME, block, true);

    final BasicBlocksGenerator basicBlocksGenerator =
      new BasicBlocksGenerator(factory, oldMethodName, frameClassName, frameVarName, blockFieldName, stackVarName, returnType);
    block.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator.Pair> blocks = basicBlocksGenerator.getBlocks();

    blocks.forEach(codeBlock -> replaceReturnStatements(factory, codeBlock.getBlock(), stackVarName, retVarName));

    final String casesString = blocks.stream()
      .map(section -> "case " + section.getId() + ": " + section.getBlock().getText())
      .collect(Collectors.joining(""));

    final PsiCodeBlock body = factory.createCodeBlock();

    body.add(createStackDeclaration(project, factory, frameClassName, stackVarName));
    final String arguments =
      Arrays.stream(oldMethod.getParameterList().getParameters()).map(PsiNamedElement::getName).collect(Collectors.joining(","));
    body.add(createAddStatement(factory, frameClassName, stackVarName, arguments));
    addRetDeclaration(factory, body, returnType, retVarName);
    body.add(factory.createStatementFromText("while (true) {" +
                                             frameClassName +
                                             " " +
                                             frameVarName +
                                             " = " +
                                             stackVarName +
                                             ".get(" +
                                             stackVarName +
                                             ".size() - 1);" +
                                             "switch (" +
                                             frameVarName +
                                             "." +
                                             blockFieldName +
                                             ") {" +
                                             casesString +
                                             "} }", null));

    return body;
  }

  private static void replaceIdentifierWithFrameAccess(@NotNull final PsiElementFactory factory,
                                                       @NotNull final String frameVarName,
                                                       @NotNull final List<Variable> variables,
                                                       @NotNull final PsiCodeBlock block) {
    for (final PsiReferenceExpression expression : Visitors.extractReferenceExpressions(block)) {
      final PsiElement element = expression.resolve();
      if (element instanceof PsiLocalVariable) {
        final PsiLocalVariable variable = (PsiLocalVariable)element;
        if (variables.stream().anyMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
          final PsiElement nameElement = expression.getReferenceNameElement();
          nameElement.replace(factory.createExpressionFromText(frameVarName + "." + nameElement.getText(), null));
        }
      }
      if (element instanceof PsiParameter) {
        final PsiParameter parameter = (PsiParameter)element;
        if (variables.stream().anyMatch(variable1 -> variable1.getName().equals(parameter.getName()))) {
          final PsiElement nameElement = expression.getReferenceNameElement();
          nameElement.replace(factory.createExpressionFromText(frameVarName + "." + nameElement.getText(), null));
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
      final List<PsiStatement> statements = new ArrayList<>();
      if (hasExpression) {
        statements.add(factory.createStatementFromText(retVarName + " = " + returnValue.getText() + ";", null));
      }
      statements.add(factory.createStatementFromText(
        "if (" + stackVarName + ".size() == 1)\n" + "return " + (hasExpression ? retVarName : "") + ";\n", null));
      statements.add(factory.createStatementFromText(stackVarName + ".remove(" + stackVarName + ".size() - 1);", null));
      statements.add(factory.createStatementFromText("break;", null));
      PsiElement anchor = statement;
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock.class, true);
      for (PsiStatement statement1 : statements) {
        anchor = parentBlock.addAfter(statement1, anchor);
      }
      statement.delete();
    }
  }

  private static void replaceDeclarationsWithInitializersWithAssignments(@NotNull final PsiElementFactory factory,
                                                                         @NotNull final String frameVarName,
                                                                         @NotNull final PsiCodeBlock block) {
    for (final PsiDeclarationStatement statement : Visitors.extractDeclarationStatements(block)) {
      final PsiElement[] elements = statement.getDeclaredElements();
      final List<PsiStatement> assignments = new ArrayList<>();
      for (final PsiElement element : elements) {
        if (!(element instanceof PsiLocalVariable)) {
          continue;
        }
        final PsiLocalVariable variable = (PsiLocalVariable)element;
        if (!variable.hasInitializer()) {
          continue;
        }
        assignments.add(factory.createStatementFromText(
          frameVarName + "." + variable.getName() + " = " + variable.getInitializer().getText() + ";", null));
      }
      final PsiElement parent = statement.getParent();
      if (!(parent instanceof PsiCodeBlock)) {
        continue;
      }
      final PsiCodeBlock parentBlock = (PsiCodeBlock)parent;
      PsiElement anchor = statement;
      for (PsiStatement assignment : assignments) {
        anchor = parentBlock.addAfter(assignment, anchor);
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

  private static void addRetDeclaration(@NotNull final PsiElementFactory factory,
                                        @NotNull final PsiCodeBlock body,
                                        @NotNull final PsiType returnType,
                                        @NotNull final String retVarName) {
    if (returnType instanceof PsiPrimitiveType && PsiPrimitiveType.VOID.equals(returnType)) {
      return;
    }
    body.add(factory.createStatementFromText(returnType.getPresentableText() + " " + retVarName + " = " + getInitialValue(returnType) + ";",
                                             null));
  }

  private static void extractRecursiveCallsToStatements(@NotNull final PsiElementFactory factory,
                                                        @NotNull final JavaCodeStyleManager styleManager,
                                                        @NotNull final PsiCodeBlock block,
                                                        @NotNull final String oldMethodName,
                                                        @NotNull final PsiType returnType,
                                                        @NotNull final List<Variable> variables) {
    for (PsiMethodCallExpression call : Visitors.extractRecursiveCalls(block, oldMethodName)) {
      final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
      if (parentStatement == call.getParent() && parentStatement instanceof PsiExpressionStatement) {
        continue;
      }
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
      final String temp = styleManager.suggestUniqueVariableName(Constants.TEMP, block, true);
      variables.add(new Variable(temp, returnType.getPresentableText()));
      parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
      call.replace(factory.createExpressionFromText(temp, null));
    }
  }

  @NotNull
  static <T extends PsiElement> PsiStatement createAddStatement(@NotNull final PsiElementFactory factory,
                                                                @NotNull final String frameClassName,
                                                                @NotNull final String stackVarName,
                                                                @NotNull final String arguments) {
    return factory.createStatementFromText(stackVarName + ".add(new " + frameClassName + "(" + arguments + "));", null);
  }

  @NotNull
  private static PsiElement createStackDeclaration(@NotNull final Project project,
                                                   @NotNull final PsiElementFactory factory,
                                                   @NotNull final String frameClassName,
                                                   @NotNull final String stackVarName) {
    final PsiStatement declarationStatement = factory.createStatementFromText(
      "java.util.List<" + frameClassName + "> " + stackVarName + " = new java.util.ArrayList<>();", null);
    return JavaCodeStyleManager.getInstance(project).shortenClassReferences(declarationStatement);
  }
}
