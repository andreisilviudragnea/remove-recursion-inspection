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
  @NotNull private final Block myCurrentBlock;

  BreakContinueReplacerVisitor(@NotNull final Map<PsiStatement, Block> breakTargets,
                               @NotNull final Map<PsiStatement, Block> continueTargets,
                               @NotNull final PsiElementFactory factory,
                               @NotNull final Block currentBlock) {
    myBreakTargets = breakTargets;
    myContinueTargets = continueTargets;
    myFactory = factory;
    myCurrentBlock = currentBlock;
  }

  private void replaceWithUnconditionalJump(@NotNull final PsiStatement targetStatement,
                                            @NotNull final Map<PsiStatement, Block> targets,
                                            @NotNull final PsiStatement statement) {
    final Block block = targets.get(targetStatement);
    if (block == null) {
      return;
    }
    block.addReference(Ref.create(block));
    block.setDoNotInline(true);
    myCurrentBlock.addChild(block);
    statement.getParent().addBefore(myFactory.createStatementFromText("frame.block = " + block.getId() + ";", null), statement);
    statement.replace(myFactory.createStatementFromText("break;", null));
  }

  @Override
  public void visitBreakStatement(PsiBreakStatement statement) {
    final PsiStatement exitedStatement = statement.findExitedStatement();
    if (exitedStatement == null) {
      return;
    }
    replaceWithUnconditionalJump(exitedStatement, myBreakTargets, statement);
  }

  @Override
  public void visitContinueStatement(PsiContinueStatement statement) {
    final PsiStatement continuedStatement = statement.findContinuedStatement();
    if (continuedStatement == null) {
      return;
    }
    replaceWithUnconditionalJump(continuedStatement, myContinueTargets, statement);
  }
}
