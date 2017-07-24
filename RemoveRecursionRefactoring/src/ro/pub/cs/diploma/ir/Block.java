package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiStatement;

import java.util.ArrayList;
import java.util.List;

public class Block implements Statement {
  private final int id;
  private boolean inline;
  private boolean afterRecursiveCall;
  private final List<Statement> statements = new ArrayList<>();
  private final List<Block> references = new ArrayList<>();

  public Block(int id) {
    this.id = id;
  }

  public void add(Statement statement) {
    statements.add(statement);
  }

  public void add(PsiStatement statement) {
    statements.add(new NormalStatement(statement));
  }

  public void addReference(Block block) {
    references.add(block);
  }

  public boolean isFinished() {
    return statements.size() != 0 && statements.get(statements.size() - 1) instanceof TerminatorStatement;
  }

  public int getId() {
    return id;
  }

  public List<Statement> getStatements() {
    return statements;
  }

  public void checkInline() {
    if (references.size() == 1 && !afterRecursiveCall) {
      inline = true;
    }
  }

  public boolean isInline() {
    return inline;
  }

  public boolean isAfterRecursiveCall() {
    return afterRecursiveCall;
  }

  public void setAfterRecursiveCall(boolean afterRecursiveCall) {
    this.afterRecursiveCall = afterRecursiveCall;
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
