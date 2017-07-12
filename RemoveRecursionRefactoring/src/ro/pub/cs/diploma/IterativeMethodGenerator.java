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
  static PsiCodeBlock createIterativeBody(Project project, PsiElementFactory factory, PsiMethod oldMethod, List<Variable> variables) {
    final String name = oldMethod.getName();
    final String contextClassName = ContextClassGenerator.getContextClassName(name);
    final PsiType returnType = oldMethod.getReturnType();
    if (returnType == null) {
      return null;
    }

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

    Visitors.replaceSingleStatementsWithBlockStatements(factory, block);
    extractRecursiveCallsToStatements(factory, block, name, returnType, variables);

    replaceIdentifierWithContextAccess(factory, variables, block);
    replaceDeclarationsWithInitializersWithAssignments(factory, block);

    final BasicBlocksGenerator basicBlocksGenerator = new BasicBlocksGenerator(factory, name, contextClassName, returnType);
    block.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator.Pair> blocks = basicBlocksGenerator.getBlocks();

    blocks.forEach(codeBlock -> replaceReturnStatements(factory, codeBlock.getBlock()));

    final String casesString = blocks.stream()
      .map(section -> "case " + section.getId() + ": " + section.getBlock().getText())
      .collect(Collectors.joining(""));

    final PsiCodeBlock body = factory.createCodeBlock();

    body.add(createStackDeclaration(project, factory, contextClassName));
    body.add(createAddStatement(factory, contextClassName, oldMethod));
    addRetDeclaration(factory, body, returnType);
    body.add(factory.createStatementFromText("while (true) {" +
                                             contextClassName + " context = stack.get(stack.size() - 1);" +
                                             "switch (context.section) {" + casesString + "} }", null));

    return body;
  }

  private static void replaceIdentifierWithContextAccess(PsiElementFactory factory, List<Variable> variables,
                                                         PsiCodeBlock block) {
    for (PsiReferenceExpression expression : Visitors.extractReferenceExpressions(block)) {
      final PsiElement element = expression.resolve();
      if (element instanceof PsiLocalVariable) {
        PsiLocalVariable variable = (PsiLocalVariable)element;
        if (variables.stream().anyMatch(variable1 -> variable1.getName().equals(variable.getName()))) {
          final PsiElement nameElement = expression.getReferenceNameElement();
          nameElement.replace(
            factory.createExpressionFromText("context." + nameElement.getText(), null));
        }
      }
      if (element instanceof PsiParameter) {
        PsiParameter parameter = (PsiParameter)element;
        if (variables.stream().anyMatch(variable1 -> variable1.getName().equals(parameter.getName()))) {
          final PsiElement nameElement = expression.getReferenceNameElement();
          nameElement.replace(
            factory.createExpressionFromText("context." + nameElement.getText(), null));
        }
      }
    }
  }

  private static void replaceReturnStatements(final PsiElementFactory factory, final PsiCodeBlock block) {
    for (final PsiReturnStatement statement : Visitors.extractReturnStatements(block)) {
      final PsiExpression returnValue = statement.getReturnValue();
      final boolean hasExpression = returnValue != null;
      final List<PsiStatement> statements = new ArrayList<>();
      if (hasExpression) {
        statements.add(factory.createStatementFromText("ret = " + returnValue.getText() + ";", null));
      }
      statements.add(factory.createStatementFromText(
        "if (stack.size() == 1)\nreturn " + (hasExpression ? "ret" : "") + "; else\nstack.remove(stack.size() - 1);", null));
      statements.add(factory.createStatementFromText("break;", null));
      PsiElement anchor = statement;
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock.class, true);
      for (PsiStatement statement1 : statements) {
        anchor = parentBlock.addAfter(statement1, anchor);
      }
      statement.delete();
    }
  }

  private static void replaceDeclarationsWithInitializersWithAssignments(final PsiElementFactory factory,
                                                                         final PsiCodeBlock block) {
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
        assignments.add(factory.createStatementFromText("context." +
                                                        variable.getName() + " = " + variable.getInitializer().getText() + ";", null));
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
                                        @NotNull final PsiType returnType) {
    if (returnType instanceof PsiPrimitiveType && PsiPrimitiveType.VOID.equals(returnType)) {
      return;
    }
    body.add(factory.createStatementFromText(returnType.getPresentableText() + " ret = " + getInitialValue(returnType) + ";", null));
  }

  private static void extractRecursiveCallsToStatements(PsiElementFactory factory, PsiCodeBlock block, String name,
                                                        PsiType returnType, List<Variable> variables) {
    int count = 0;
    for (PsiMethodCallExpression call : Visitors.extractRecursiveCalls(block, name)) {
      final PsiStatement parentStatement = PsiTreeUtil.getParentOfType(call, PsiStatement.class, true);
      if (parentStatement == call.getParent() && parentStatement instanceof PsiExpressionStatement) {
        continue;
      }
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(call, PsiCodeBlock.class, true);
      final String temp = "temp" + count++;
      variables.add(new Variable(temp, returnType.getPresentableText()));
      parentBlock.addBefore(factory.createVariableDeclarationStatement(temp, returnType, call), parentStatement);
      call.replace(factory.createExpressionFromText(temp, null));
    }
  }

  @NotNull
  private static PsiStatement createAddStatement(@NotNull final PsiElementFactory factory,
                                                 @NotNull final String contextClassName,
                                                 @NotNull final PsiMethod method) {
    final String arguments = Arrays.stream(method.getParameterList().getParameters())
      .map(PsiNamedElement::getName).collect(Collectors.joining(","));
    return factory.createStatementFromText("stack.add(new " + contextClassName + "(" + arguments + "));", null);
  }

  @NotNull
  private static PsiElement createStackDeclaration(@NotNull final Project project,
                                                   @NotNull final PsiElementFactory factory,
                                                   @NotNull final String contextClassName) {
    final PsiStatement declarationStatement = factory.createStatementFromText(
      "java.util.List<" + contextClassName + "> stack = new java.util.ArrayList<>();", null);
    return JavaCodeStyleManager.getInstance(project).shortenClassReferences(declarationStatement);
  }
}
