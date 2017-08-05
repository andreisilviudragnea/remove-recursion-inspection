package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Block implements Statement {
  private final int id;
  @NotNull private final List<Statement> statements = new ArrayList<>();
  @NotNull private final List<Ref<Block>> references = new ArrayList<>();

  private boolean afterRecursiveCall;

  public Block(final int id) {
    this.id = id;
  }

  public void add(@NotNull final Statement statement) {
    statements.add(statement);
  }

  public void add(@NotNull final PsiStatement statement) {
    statements.add(new NormalStatement(statement));
  }

  public void addReference(@NotNull final Ref<Block> blockRef) {
    references.add(blockRef);
  }

  void removeReference(@NotNull final Ref<Block> blockRef) {
    references.remove(blockRef);
  }

  public boolean isFinished() {
    return statements.size() != 0 && statements.get(statements.size() - 1) instanceof TerminatorStatement;
  }

  public boolean isInlinable() {
    return references.size() == 1 && !afterRecursiveCall;
  }

  public boolean removeIfUnreachable() {
    if (id != 0 && references.size() == 0) {
      if (statements.size() == 0) {
        return false;
      }
      final Statement statement = statements.get(statements.size() - 1);
      if (statement instanceof JumpStatement) {
        ((JumpStatement)statement).detach();
        return false;
      }
      if (statement instanceof ReturnStatement) {
        return false;
      }
    }
    return true;
  }

  public int getId() {
    return id;
  }

  @NotNull
  List<Statement> getStatements() {
    return statements;
  }

  public void setAfterRecursiveCall(final boolean afterRecursiveCall) {
    this.afterRecursiveCall = afterRecursiveCall;
  }

  public boolean inlineIfTrivial() {
    if (id != 0 && statements.size() == 1 && statements.get(0) instanceof UnconditionalJumpStatement) {
      final Block jumpBlock = ((UnconditionalJumpStatement) statements.get(0)).getBlock();
      for (final Ref<Block> reference : references) {
        reference.set(jumpBlock);
      }
      if (afterRecursiveCall) {
        jumpBlock.afterRecursiveCall = true;
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
