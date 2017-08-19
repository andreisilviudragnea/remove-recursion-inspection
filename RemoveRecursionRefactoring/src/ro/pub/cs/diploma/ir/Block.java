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
  @NotNull private final List<Ref<Block>> references = new ArrayList<>();
  @NotNull private final List<Block> children = new ArrayList<>();

  private boolean doNotInline;
  private boolean finished;

  public Block(final int id) {
    this.id = id;
  }

  public void addConditionalJump(@NotNull final PsiExpression condition,
                                 @NotNull final Ref<Block> thenBlockRef,
                                 @NotNull final Ref<Block> elseBlockRef) {
    statements.add(new ConditionalJumpStatement(condition, thenBlockRef, elseBlockRef));
    children.add(thenBlockRef.get());
    children.add(elseBlockRef.get());
    finished = true;
  }

  public void addUnconditionalJump(@NotNull final Ref<Block> blockRef) {
    statements.add(new UnconditionalJumpStatement(blockRef));
    children.add(blockRef.get());
    finished = true;
  }

  public void addReturnStatement(@NotNull final PsiReturnStatement statement) {
    statements.add(new ReturnStatement(statement));
    finished = true;
  }

  public void add(@NotNull final PsiStatement statement) {
    statements.add(new NormalStatement(statement));
  }

  public void addReference(@NotNull final Ref<Block> blockRef) {
    references.add(blockRef);
  }

  public void addChild(@NotNull final Block child) {
    children.add(child);
  }

  public boolean isFinished() {
    return finished;
  }

  public boolean isInlinable() {
    return references.size() == 1 && !doNotInline;
  }

  @NotNull
  public List<Block> getChildren() {
    return children;
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
      for (final Ref<Block> reference : references) {
        reference.set(jumpBlock);
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
