package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.passes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IterativeMethodGenerator {
  @NotNull private final PsiElementFactory myFactory;
  @NotNull private final JavaCodeStyleManager myStyleManager;
  @NotNull private final PsiMethod myMethod;
  @NotNull private final NameManager myNameManager;

  private IterativeMethodGenerator(@NotNull PsiElementFactory factory,
                                   @NotNull JavaCodeStyleManager styleManager,
                                   @NotNull PsiMethod method, @NotNull NameManager nameManager) {
    myFactory = factory;
    myStyleManager = styleManager;
    myMethod = method;
    myNameManager = nameManager;
  }

  @NotNull
  static IterativeMethodGenerator getInstance(@NotNull final PsiElementFactory factory,
                                              @NotNull final JavaCodeStyleManager styleManager,
                                              @NotNull final PsiMethod method,
                                              @NotNull final NameManager nameManager) {
    return new IterativeMethodGenerator(factory, styleManager, method, nameManager);
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

    AddFrameClass.getInstance(myMethod, myNameManager).apply(myMethod);

    final PsiCodeBlock incorporatedBody = IncorporateBody.getInstance(myNameManager, myFactory, myStyleManager).apply(myMethod);
    if (incorporatedBody == null) {
      return;
    }

    replaceIdentifierWithFrameAccess(myMethod, incorporatedBody, myNameManager);
    Passes.replaceDeclarationsWithInitializersWithAssignments(myMethod, incorporatedBody, myNameManager);

    final BasicBlocksGenerator2 basicBlocksGenerator = new BasicBlocksGenerator2(myMethod, myNameManager);
    incorporatedBody.accept(basicBlocksGenerator);
    final List<BasicBlocksGenerator2.Pair> pairs = basicBlocksGenerator.getBlocks();

    final Ref<Boolean> atLeastOneLabeledBreak = new Ref<>(false);
    pairs.forEach(pair -> replaceReturnStatements(pair.getBlock(), myNameManager, atLeastOneLabeledBreak));

    final String casesString = pairs.stream().map(pair -> "case " + pair.getId() + ":" + pair.getBlock().getText())
      .collect(Collectors.joining(""));

    incorporatedBody.replace(statement(
      (atLeastOneLabeledBreak.get() ? myNameManager.getSwitchLabelName() + ":" : "") +
      "switch(" + myNameManager.getFrameVarName() + "." + myNameManager.getBlockFieldName() + "){" + casesString + "}"));
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
}
