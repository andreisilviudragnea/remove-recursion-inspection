package ro.pub.cs.diploma;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

class Visitors {
  static List<PsiReturnStatement> extractReturnStatements(PsiCodeBlock block) {
    final List<PsiReturnStatement> returnStatements = new ArrayList<>();
    block.accept(new JavaRecursiveElementWalkingVisitor() {
      @Override
      public void visitReturnStatement(PsiReturnStatement statement) {
        super.visitReturnStatement(statement);
        returnStatements.add(statement);
      }

      @Override
      public void visitClass(PsiClass aClass) {
      }

      @Override
      public void visitLambdaExpression(PsiLambdaExpression expression) {
      }
    });
    return returnStatements;
  }
}
