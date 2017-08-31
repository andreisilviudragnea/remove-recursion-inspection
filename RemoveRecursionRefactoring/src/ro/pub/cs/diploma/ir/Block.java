package ro.pub.cs.diploma.ir;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Block implements Statement {
  private final int id;
  @NotNull private final List<Statement> myStatements = new ArrayList<>();
  @NotNull private final List<Ref<Block>> myInBlocks = new ArrayList<>();
  @NotNull private final List<Ref<Block>> myOutBlocks = new ArrayList<>();

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
    myStatements.add(new ConditionalJumpStatement(condition, thenBlockRef, elseBlockRef));
    addEdgeTo(thenBlockRef);
    addEdgeTo(elseBlockRef);
    finished = true;
  }

  public void addUnconditionalJump(@NotNull final Ref<Block> blockRef) {
    if (finished) {
      return;
    }
    myStatements.add(new UnconditionalJumpStatement(blockRef));
    addEdgeTo(blockRef);
    finished = true;
  }

  public void addReturnStatement(@NotNull final PsiReturnStatement statement) {
    myStatements.add(new ReturnStatement(statement));
    finished = true;
  }

  public void addSwitchStatement(@NotNull final PsiExpression expression, @NotNull final List<Statement> statements) {
    myStatements.add(new SwitchStatement(expression, statements));
    for (Statement statement : statements) {
      if (statement instanceof UnconditionalJumpStatement) {
        addEdgeTo(((UnconditionalJumpStatement)statement).getBlockRef());
      }
    }
    finished = true;
  }

  public void add(@NotNull final PsiStatement statement) {
    myStatements.add(new NormalStatement(statement));
  }

  public void addEdgeTo(@NotNull final Ref<Block> blockRef) {
    myOutBlocks.add(blockRef);
    blockRef.get().myInBlocks.add(blockRef);
  }

  public boolean isInlinable() {
    return myInBlocks.size() == 1 && !doNotInline;
  }

  public boolean isFinished() {
    return finished;
  }

  @NotNull
  public List<Ref<Block>> getOutBlocks() {
    return myOutBlocks;
  }

  public int getId() {
    return id;
  }

  @NotNull
  List<Statement> getStatements() {
    return myStatements;
  }

  public void setDoNotInline(final boolean doNotInline) {
    this.doNotInline = doNotInline;
  }

  public boolean inlineIfTrivial() {
    if (id != 0 && myStatements.size() == 1 && myStatements.get(0) instanceof UnconditionalJumpStatement) {
      final Block jumpBlock = ((UnconditionalJumpStatement)myStatements.get(0)).getBlock();
      for (final Ref<Block> inBlock : myInBlocks) {
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

  public List<String> toDot() {
    List<String> strings = new ArrayList<>();
    if (myStatements.size() > 0) {
      for (Statement statement : myStatements.subList(0, myStatements.size() - 1)) {
        strings.add(((WrapperStatement)statement).getStatement().getText()
                      .replace("{", "\\{")
                      .replace("}", "\\}")
                      .replace("<", "\\<")
                      .replace(">", "\\>")
                      .replace("\n", "\\n"));
      }
    }

    List<String> strings2 = new ArrayList<>();
    strings2.add(String.format("id: %s", id));
    strings2.add(strings.stream().collect(Collectors.joining("\\n")));
    String color = doNotInline ? "color=red" : "";
    List<String> statements = new ArrayList<>();
    if (myStatements.size() > 0) {
      final Statement lastStatement = myStatements.get(myStatements.size() - 1);
      if (lastStatement instanceof ConditionalJumpStatement) {
        strings2.add(((ConditionalJumpStatement)lastStatement).getCondition().getText()
                         .replace("{", "\\{")
                         .replace("}", "\\}")
                         .replace("<", "\\<")
                         .replace(">", "\\>")
                         .replace("\n", "\\n"));
        strings2.add("{<true>true|<false>false}");
        statements.add(String.format("%s:true -> %s;", id, ((ConditionalJumpStatement)lastStatement).getThenBlock().getId()));
        statements.add(String.format("%s:false -> %s;", id, ((ConditionalJumpStatement)lastStatement).getElseBlock().getId()));
      }
      else if (lastStatement instanceof UnconditionalJumpStatement) {
        final int jumpId = ((UnconditionalJumpStatement)lastStatement).getBlock().getId();
        strings2.add(String.format("jump %s;", jumpId));
        statements.add(String.format("%s -> %s;", id, jumpId));
      } else if (lastStatement instanceof ReturnStatement) {
        strings2.add(((ReturnStatement)lastStatement).getStatement().getText()
                       .replace("{", "\\{")
                       .replace("}", "\\}")
                       .replace("<", "\\<")
                       .replace(">", "\\>")
                       .replace("\n", "\\n"));
      }
    }
    statements.add(String.format("%s [label=\"{%s}\" %s];", id, strings2.stream().collect(Collectors.joining("|")), color));

    return statements;
  }
}
