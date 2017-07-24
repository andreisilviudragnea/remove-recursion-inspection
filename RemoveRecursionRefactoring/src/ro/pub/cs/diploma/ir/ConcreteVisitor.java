package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiStatement;

public class ConcreteVisitor implements Visitor {
  private final PsiElementFactory factory;
  private final String blockSet;
  private final PsiCodeBlock block;

  private PsiCodeBlock currentBlock;

  private PsiCodeBlock newBlock() {
    return factory.createCodeBlock();
  }

  private void newStatement(String text) {
    currentBlock.add(factory.createStatementFromText(text, null));
  }

  private void newBreakStatement() {
    newStatement("break;");
  }

  public ConcreteVisitor(PsiElementFactory factory, String frameVarName, String blockFieldName) {
    this.factory = factory;
    blockSet = frameVarName + "." + blockFieldName + " = ";
    block = newBlock();

    currentBlock = block;
  }

  public PsiCodeBlock getBlock() {
    return block;
  }

  @Override
  public void visit(Block block) {
    for (Statement statement : block.getStatements()) {
      statement.accept(this);
    }
  }

  @Override
  public void visit(ConditionalJumpStatement conditionalJumpStatement) {
    final PsiCodeBlock oldCurrentBlock = currentBlock;

    final Block thenBlock = conditionalJumpStatement.getThenBlock();
    PsiCodeBlock thenPsiBlock = null;
    if (thenBlock.isInline()) {
      thenPsiBlock = newBlock();
      currentBlock = thenPsiBlock;
      thenBlock.accept(this);
    }

    final Block elseBlock = conditionalJumpStatement.getElseBlock();
    PsiCodeBlock elsePsiBlock = null;
    if (elseBlock.isInline()) {
      elsePsiBlock = newBlock();
      currentBlock = elsePsiBlock;
      elseBlock.accept(this);
    }

    currentBlock = oldCurrentBlock;
    final String conditionText = conditionalJumpStatement.getCondition().getText();
    if (thenPsiBlock == null && elsePsiBlock == null) {
      newStatement(blockSet + conditionText + " ? " + thenBlock.getId() + " : " + elseBlock.getId() + ";");
      newBreakStatement();
      return;
    }

    final PsiCodeBlock concreteThenPsiBlock;
    if (thenPsiBlock != null) {
      concreteThenPsiBlock = thenPsiBlock;
    }
    else {
      concreteThenPsiBlock = newBlock();
      currentBlock = concreteThenPsiBlock;
      newStatement(blockSet + thenBlock.getId() + ";");
      newBreakStatement();
      currentBlock = oldCurrentBlock;
    }

    final PsiCodeBlock concreteElsePsiBlock;
    if (elsePsiBlock != null) {
      concreteElsePsiBlock = elsePsiBlock;
    }
    else {
      concreteElsePsiBlock = newBlock();
      currentBlock = concreteElsePsiBlock;
      newStatement(blockSet + elseBlock.getId() + ";");
      newBreakStatement();
      currentBlock = oldCurrentBlock;
    }

    newStatement("if (" + conditionText + ")" + concreteThenPsiBlock.getText() + " else " + concreteElsePsiBlock.getText());
  }

  @Override
  public void visit(NormalStatement normalStatement) {
    currentBlock.add(normalStatement.getStatement());
  }

  @Override
  public void visit(ReturnStatement returnStatement) {
    currentBlock.add(returnStatement.getStatement());
  }

  @Override
  public void visit(UnconditionalJumpStatement unconditionalJumpStatement) {
    final PsiCodeBlock oldCurrentBlock = currentBlock;

    final Block block = unconditionalJumpStatement.getBlock();

    PsiCodeBlock newBlock = null;
    if (block.isInline()) {
      newBlock = newBlock();
      currentBlock = newBlock;
      block.accept(this);
      currentBlock = oldCurrentBlock;
    }

    if (newBlock != null) {
      for (PsiStatement statement : newBlock.getStatements()) {
        currentBlock.add(statement);
      }
      return;
    }

    newStatement(blockSet + block.getId() + ";");
    newBreakStatement();
  }
}
