package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.ir.Block;

import java.util.Map;

public class BreakReplacerVisitor extends JavaRecursiveElementVisitor {
  @NotNull private final Map<PsiStatement, Block> myBreakTargets;
  @NotNull private final PsiElementFactory myFactory;

  BreakReplacerVisitor(@NotNull final Map<PsiStatement, Block> breakTargets, @NotNull final PsiElementFactory factory) {
    myBreakTargets = breakTargets;
    myFactory = factory;
  }

  @Override
  public void visitBreakStatement(PsiBreakStatement statement) {
    final PsiStatement exitedStatement = statement.findExitedStatement();
    if (exitedStatement == null) {
      return;
    }
    final Block block = myBreakTargets.get(exitedStatement);
    if (block == null) {
      return;
    }
    block.addReference(Ref.create(block));
    final PsiElement parent = statement.getParent();
    parent.addBefore(myFactory.createStatementFromText("frame.block = " + block.getId() + ";", null), statement);
    statement.replace(myFactory.createStatementFromText("break;", null));
  }
}
