package ro.pub.cs.diploma.passes;

import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.Refactorings;
import ro.pub.cs.diploma.Util;

import java.util.List;

public class ReplaceForEachLoopsWithIndexedForLoops implements Pass<PsiMethod, List<PsiForeachStatement>, Object> {
  @NotNull private final PsiMethod myMethod;

  private ReplaceForEachLoopsWithIndexedForLoops(@NotNull PsiMethod method) {
    myMethod = method;
  }

  @NotNull
  public static ReplaceForEachLoopsWithIndexedForLoops getInstance(@NotNull PsiMethod method) {
    return new ReplaceForEachLoopsWithIndexedForLoops(method);
  }

  @Override
  public List<PsiForeachStatement> collect(PsiMethod method) {
    return Util.INSTANCE.getPsiForEachStatements(method);
  }

  @Override
  public Object transform(List<PsiForeachStatement> statements) {
    statements.forEach(statement -> Refactorings.replaceForEachLoopWithIndexedForLoop(statement, myMethod));
    return null;
  }
}
