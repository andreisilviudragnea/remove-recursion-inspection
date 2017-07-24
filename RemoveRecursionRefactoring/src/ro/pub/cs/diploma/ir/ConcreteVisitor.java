package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private void newBlockSet(String val) {
    newStatement(blockSet + val + ";");
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

  @Nullable
  private PsiCodeBlock inline(Block block) {
    final PsiCodeBlock oldCurrentBlock = currentBlock;
    PsiCodeBlock psiBlock = null;
    if (block.isInline()) {
      psiBlock = newBlock();
      currentBlock = psiBlock;
      block.accept(this);
      currentBlock = oldCurrentBlock;
    }
    return psiBlock;
  }

  @NotNull
  private PsiCodeBlock getConcreteBlock(Block block, PsiCodeBlock psiBlock) {
    PsiCodeBlock concretePsiBlock;
    if (psiBlock != null) {
      concretePsiBlock = psiBlock;
    }
    else {
      concretePsiBlock = newBlock();
      final PsiCodeBlock oldCurrentBlock = currentBlock;
      currentBlock = concretePsiBlock;
      newBlockSet(String.valueOf(block.getId()));
      currentBlock = oldCurrentBlock;
    }
    return concretePsiBlock;
  }

  @Override
  public void visit(ConditionalJumpStatement conditionalJumpStatement) {
    final Block thenBlock = conditionalJumpStatement.getThenBlock();
    final PsiCodeBlock thenPsiBlock = inline(thenBlock);

    final Block elseBlock = conditionalJumpStatement.getElseBlock();
    final PsiCodeBlock elsePsiBlock = inline(elseBlock);

    final String conditionText = conditionalJumpStatement.getCondition().getText();

    if (thenPsiBlock == null && elsePsiBlock == null) {
      newBlockSet(conditionText + " ? " + thenBlock.getId() + " : " + elseBlock.getId());
      return;
    }

    final PsiCodeBlock concreteThenPsiBlock = getConcreteBlock(thenBlock, thenPsiBlock);
    final PsiCodeBlock concreteElsePsiBlock = getConcreteBlock(elseBlock, elsePsiBlock);

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
    final Block block = unconditionalJumpStatement.getBlock();

    final PsiCodeBlock psiBlock = inline(block);

    if (psiBlock != null) {
      for (PsiStatement statement : psiBlock.getStatements()) {
        currentBlock.add(statement);
      }
      return;
    }

    newBlockSet(String.valueOf(block.getId()));
  }
}
