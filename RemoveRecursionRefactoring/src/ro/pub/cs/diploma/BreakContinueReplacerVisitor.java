package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.ir.Block;

import java.util.Map;

public class BreakContinueReplacerVisitor extends JavaRecursiveElementVisitor {
  @NotNull private final Map<PsiStatement, Block> myBreakTargets;
  @NotNull private final Map<PsiStatement, Block> myContinueTargets;
  @NotNull private final PsiElementFactory myFactory;

  BreakContinueReplacerVisitor(@NotNull final Map<PsiStatement, Block> breakTargets,
                               @NotNull final Map<PsiStatement, Block> continueTargets,
                               @NotNull final PsiElementFactory factory) {
    myBreakTargets = breakTargets;
    myContinueTargets = continueTargets;
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
    statement.getParent().addBefore(myFactory.createStatementFromText("frame.block = " + block.getId() + ";", null), statement);
    statement.replace(myFactory.createStatementFromText("break;", null));
  }

  @Override
  public void visitContinueStatement(PsiContinueStatement statement) {
    final PsiStatement continuedStatement = statement.findContinuedStatement();
    if (continuedStatement == null) {
      return;
    }
    final Block block = myContinueTargets.get(continuedStatement);
    if (block == null) {
      return;
    }
    block.addReference(Ref.create(block));
    statement.getParent().addBefore(myFactory.createStatementFromText("frame.block = " + block.getId() + ";", null), statement);
    statement.replace(myFactory.createStatementFromText("break;", null));
  }
}
