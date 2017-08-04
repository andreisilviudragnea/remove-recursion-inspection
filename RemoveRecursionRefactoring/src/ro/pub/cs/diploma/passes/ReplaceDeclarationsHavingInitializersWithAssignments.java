package ro.pub.cs.diploma.passes;

import com.intellij.psi.*;
import com.intellij.refactoring.util.RefactoringUtil;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.NameManager;
import ro.pub.cs.diploma.RecursionUtil;
import ro.pub.cs.diploma.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReplaceDeclarationsHavingInitializersWithAssignments implements Pass<PsiCodeBlock, List<PsiDeclarationStatement>, Object> {
  @NotNull private final PsiMethod myMethod;
  @NotNull private final NameManager myNameManager;
  @NotNull private final PsiElementFactory myFactory;

  private ReplaceDeclarationsHavingInitializersWithAssignments(@NotNull final PsiMethod method,
                                                               @NotNull final NameManager nameManager,
                                                               @NotNull final PsiElementFactory factory) {
    myMethod = method;
    myNameManager = nameManager;
    myFactory = factory;
  }

  @NotNull
  public static ReplaceDeclarationsHavingInitializersWithAssignments getInstance(@NotNull final PsiMethod method,
                                                                                 @NotNull final NameManager nameManager,
                                                                                 @NotNull final PsiElementFactory factory) {
    return new ReplaceDeclarationsHavingInitializersWithAssignments(method, nameManager, factory);
  }

  @Override
  public List<PsiDeclarationStatement> collect(PsiCodeBlock block) {
    final List<PsiDeclarationStatement> declarations = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitDeclarationStatement(PsiDeclarationStatement statement) {
        if (RecursionUtil.hasToBeSavedOnStack(statement, myMethod)) {
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
    return declarations;
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

  @Override
  public Object transform(List<PsiDeclarationStatement> declarations) {
    for (final PsiDeclarationStatement statement : declarations) {
      final PsiElement parent = statement.getParent();
      final Stream<String> stream = getVariablesStream(statement, myNameManager.getFrameVarName());
      if (parent instanceof PsiForStatement) {
        statement.replace(Util.statement(myFactory, stream.collect(Collectors.joining(",")) + ";"));
        continue;
      }
      final PsiCodeBlock parentBlock = (PsiCodeBlock)parent;
      PsiElement anchor = statement;
      for (final String string : stream.collect(Collectors.toList())) {
        anchor = parentBlock.addAfter(Util.statement(myFactory, string + ";"), anchor);
      }
      statement.delete();
    }
    return null;
  }
}
