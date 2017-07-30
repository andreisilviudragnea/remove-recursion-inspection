package ro.pub.cs.diploma.ir;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InlineVisitor implements Visitor {
  @NotNull private final PsiElementFactory factory;
  @NotNull private final String blockSet;
  @NotNull private final PsiCodeBlock block;

  @NotNull private PsiCodeBlock currentBlock;

  @NotNull
  private PsiCodeBlock newBlock() {
    return factory.createCodeBlock();
  }

  private void newStatement(@NotNull final String text) {
    currentBlock.add(factory.createStatementFromText(text, null));
  }

  private void newBlockSet(@NotNull final String val) {
    newStatement(blockSet + val + ";");
    newStatement("break;");
  }

  public InlineVisitor(@NotNull final PsiElementFactory factory, @NotNull final String frameVarName, @NotNull final String blockFieldName) {
    this.factory = factory;
    blockSet = frameVarName + "." + blockFieldName + " = ";
    block = newBlock();

    currentBlock = block;
  }

  @NotNull
  public PsiCodeBlock getBlock() {
    return block;
  }

  @Override
  public void visit(@NotNull final Block block) {
    for (final Statement statement : block.getStatements()) {
      statement.accept(this);
    }
  }

  @Nullable
  private PsiCodeBlock inline(@NotNull final Block block) {
    @NotNull final PsiCodeBlock oldCurrentBlock = currentBlock;
    @Nullable PsiCodeBlock psiBlock = null;
    if (block.isInline()) {
      psiBlock = newBlock();
      currentBlock = psiBlock;
      block.accept(this);
      currentBlock = oldCurrentBlock;
    }
    return psiBlock;
  }

  @NotNull
  private PsiCodeBlock getConcreteBlock(@NotNull final Block block, @Nullable final PsiCodeBlock psiBlock) {
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
  public void visit(@NotNull ConditionalJumpStatement conditionalJumpStatement) {
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
  public void visit(@NotNull final NormalStatement normalStatement) {
    currentBlock.add(normalStatement.getStatement());
  }

  @Override
  public void visit(@NotNull final ReturnStatement returnStatement) {
    currentBlock.add(returnStatement.getStatement());
  }

  @Override
  public void visit(@NotNull final UnconditionalJumpStatement unconditionalJumpStatement) {
    final Block block = unconditionalJumpStatement.getBlock();

    final PsiCodeBlock psiBlock = inline(block);

    if (psiBlock != null) {
      for (final PsiStatement statement : psiBlock.getStatements()) {
        currentBlock.add(statement);
      }
      return;
    }

    newBlockSet(String.valueOf(block.getId()));
  }
}
