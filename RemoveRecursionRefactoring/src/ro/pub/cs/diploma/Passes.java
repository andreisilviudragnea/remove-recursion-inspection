package ro.pub.cs.diploma;

import com.intellij.psi.*;
import com.intellij.refactoring.util.RefactoringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Passes {

  @NotNull
  public static List<PsiForeachStatement> getPsiForEachStatements(PsiMethod method) {
    final List<PsiForeachStatement> statements = new ArrayList<>();
    method.accept(new JavaRecursiveElementVisitor() {
      @Override
      public void visitForeachStatement(PsiForeachStatement statement) {
        super.visitForeachStatement(statement);
        if (RecursionUtil.containsRecursiveCalls(statement, method)) {
          statements.add(statement);
        }
      }
    });
    return statements;
  }

  private static Stream<String> getVariablesStream(@NotNull final PsiDeclarationStatement statement, @NotNull final String frameVarName) {
    return Arrays
      .stream(statement.getDeclaredElements())
      .filter(element -> element instanceof PsiLocalVariable)
      .map(element -> (PsiLocalVariable)element)
      .filter(PsiVariable::hasInitializer)
      .map(variable -> frameVarName + "." + variable.getName() + " = " +
                       RefactoringUtil.convertInitializerToNormalExpression(variable.getInitializer(), variable.getType()).getText());
  }

  static void replaceDeclarationsWithInitializersWithAssignments(@NotNull final PsiMethod method,
                                                                 @NotNull final PsiCodeBlock block,
                                                                 @NotNull final NameManager nameManager) {
    final List<PsiDeclarationStatement> declarations = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitDeclarationStatement(PsiDeclarationStatement statement) {
        if (RecursionUtil.hasToBeSavedOnStack(statement, method)) {
          declarations.add(statement);
        }
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    final PsiElementFactory factory = Util.getFactory(block);
    for (final PsiDeclarationStatement statement : declarations) {
      final PsiElement parent = statement.getParent();
      final Stream<String> stream = getVariablesStream(statement, nameManager.getFrameVarName());
      if (parent instanceof PsiForStatement) {
        statement.replace(Util.statement(factory, stream.collect(Collectors.joining(",")) + ";"));
        continue;
      }
      final PsiCodeBlock parentBlock = (PsiCodeBlock)parent;
      PsiElement anchor = statement;
      for (final String string : stream.collect(Collectors.toList())) {
        anchor = parentBlock.addAfter(Util.statement(factory, string + ";"), anchor);
      }
      statement.delete();
    }
  }
}
