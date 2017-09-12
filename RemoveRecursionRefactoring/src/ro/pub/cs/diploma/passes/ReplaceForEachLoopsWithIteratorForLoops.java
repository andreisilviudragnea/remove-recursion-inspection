package ro.pub.cs.diploma.passes;

import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.Refactorings;
import ro.pub.cs.diploma.Util;

import java.util.List;

public class ReplaceForEachLoopsWithIteratorForLoops implements Pass<PsiMethod, List<PsiForeachStatement>, Object> {
  @NotNull private final PsiMethod myMethod;

  private ReplaceForEachLoopsWithIteratorForLoops(@NotNull PsiMethod method) {
    myMethod = method;
  }

  @NotNull
  public static ReplaceForEachLoopsWithIteratorForLoops getInstance(@NotNull PsiMethod method) {
    return new ReplaceForEachLoopsWithIteratorForLoops(method);
  }

  @Override
  public List<PsiForeachStatement> collect(PsiMethod method) {
    return Util.INSTANCE.getPsiForEachStatements(method);
  }

  @Override
  public Object transform(List<PsiForeachStatement> statements) {
    statements.forEach(statement -> Refactorings.replaceForEachLoopWithIteratorForLoop(statement, myMethod));
    return null;
  }
}
