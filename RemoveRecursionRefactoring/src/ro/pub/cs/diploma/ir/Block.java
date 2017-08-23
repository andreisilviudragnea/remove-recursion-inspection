package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Block implements Statement {
  private final int id;
  @NotNull private final List<Statement> statements = new ArrayList<>();
  @NotNull private final List<Ref<Block>> inBlocks = new ArrayList<>();
  @NotNull private final List<Ref<Block>> outBlocks = new ArrayList<>();

  private boolean doNotInline;
  private boolean finished;

  public Block(final int id) {
    this.id = id;
  }

  public void addConditionalJump(@NotNull final PsiExpression condition,
                                 @NotNull final Ref<Block> thenBlockRef,
                                 @NotNull final Ref<Block> elseBlockRef) {
    if (finished) {
      return;
    }
    statements.add(new ConditionalJumpStatement(condition, thenBlockRef, elseBlockRef));
    addEdgeTo(thenBlockRef);
    addEdgeTo(elseBlockRef);
    finished = true;
  }

  public void addUnconditionalJump(@NotNull final Ref<Block> blockRef) {
    if (finished) {
      return;
    }
    statements.add(new UnconditionalJumpStatement(blockRef));
    addEdgeTo(blockRef);
    finished = true;
  }

  public void addReturnStatement(@NotNull final PsiReturnStatement statement) {
    statements.add(new ReturnStatement(statement));
    finished = true;
  }

  public void add(@NotNull final PsiStatement statement) {
    statements.add(new NormalStatement(statement));
  }

  public void addEdgeTo(@NotNull final Ref<Block> blockRef) {
    outBlocks.add(blockRef);
    blockRef.get().inBlocks.add(blockRef);
  }

  public boolean isInlinable() {
    return inBlocks.size() == 1 && !doNotInline;
  }

  @NotNull
  public List<Ref<Block>> getOutBlocks() {
    return outBlocks;
  }

  public int getId() {
    return id;
  }

  @NotNull
  List<Statement> getStatements() {
    return statements;
  }

  public void setDoNotInline(final boolean doNotInline) {
    this.doNotInline = doNotInline;
  }

  public boolean inlineIfTrivial() {
    if (id != 0 && statements.size() == 1 && statements.get(0) instanceof UnconditionalJumpStatement) {
      final Block jumpBlock = ((UnconditionalJumpStatement)statements.get(0)).getBlock();
      for (final Ref<Block> inBlock : inBlocks) {
        inBlock.set(jumpBlock);
      }
      if (doNotInline) {
        jumpBlock.doNotInline = true;
      }
      return false;
    }
    return true;
  }

  @Override
  public void accept(@NotNull final Visitor visitor) {
    visitor.visit(this);
  }
}
