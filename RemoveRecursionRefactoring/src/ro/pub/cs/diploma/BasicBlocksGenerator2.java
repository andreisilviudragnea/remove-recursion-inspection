package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.ir.*;
import ro.pub.cs.diploma.passes.RemoveUnreachableBlocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class BasicBlocksGenerator2 extends JavaRecursiveElementVisitor {
  @NotNull private final PsiMethod method;
  @NotNull private final NameManager nameManager;

  @NotNull private final PsiElementFactory factory;
  @NotNull private final List<Block> blocks = new ArrayList<>();

  @NotNull private Block currentBlock;
  private int counter;

  BasicBlocksGenerator2(@NotNull final PsiMethod method, @NotNull final NameManager nameManager) {
    this.method = method;
    this.nameManager = nameManager;
    factory = Util.getFactory(method);
    currentBlock = newBlock();
  }

  private Block newBlock() {
    final Block block = new Block(counter++);
    blocks.add(block);
    return block;
  }

  @NotNull
  private PsiStatement statement(String text) {
    return factory.createStatementFromText(text, null);
  }

  private void addStatement(PsiStatement statement) {
    currentBlock.add(statement);
  }

  private void addStatement(String text) {
    currentBlock.add(statement(text));
  }

  private void addUnconditionalJumpStatement(Block block) {
    if (currentBlock.isFinished()) {
      return;
    }
    final Ref<Block> blockRef = Ref.create(block);
    currentBlock.add(new UnconditionalJumpStatement(blockRef));
    block.addReference(blockRef);
  }

  private void addConditionalJumpStatement(PsiExpression condition, Block thenBlock, Block jumpBlock) {
    if (currentBlock.isFinished()) {
      return;
    }
    final Ref<Block> thenBlockRef = Ref.create(thenBlock);
    final Ref<Block> jumpBlockRef = Ref.create(jumpBlock);
    currentBlock.add(new ConditionalJumpStatement(condition, thenBlockRef, jumpBlockRef));
    thenBlock.addReference(thenBlockRef);
    jumpBlock.addReference(jumpBlockRef);
  }

  private void addReturnStatement(PsiReturnStatement statement) {
    currentBlock.add(new ReturnStatement(statement));
  }

  private void processStatement(PsiStatement statement) {
    if (RecursionUtil.containsRecursiveCalls(statement, method)) {
      statement.accept(this);
    }
    else if (statement instanceof PsiReturnStatement) {
      addReturnStatement((PsiReturnStatement)statement);
    }
    else {
      addStatement(statement);
    }
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    Arrays.stream(block.getStatements()).forEach(this::processStatement);

    final PsiStatement[] statements = block.getStatements();
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID.equals(method.getReturnType()) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      addReturnStatement((PsiReturnStatement)factory.createStatementFromText("return;", null));
    }
  }

  @Override
  public void visitBlockStatement(PsiBlockStatement blockStatement) {
    Arrays.stream(blockStatement.getCodeBlock().getStatements()).forEach(this::processStatement);
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    addStatement(Util.createPushStatement(factory, nameManager.getFrameClassName(), nameManager.getStackVarName(),
                                          expression.getArgumentList().getExpressions(), PsiElement::getText));

    final Block block = newBlock();
    block.setAfterRecursiveCall(true);
    addUnconditionalJumpStatement(block);

    currentBlock = block;

    final PsiType returnType = method.getReturnType();
    if (returnType == null) {
      return;
    }
    if (!Util.isVoid(returnType)) {
      final PsiStatement parent = PsiTreeUtil.getParentOfType(expression, PsiStatement.class, true);
      expression.replace(factory.createExpressionFromText(nameManager.getRetVarName(), null));
      addStatement(parent);
    }
  }

  @Override
  public void visitIfStatement(PsiIfStatement statement) {
    final Block thenBlock = newBlock();
    final Block mergeBlock = newBlock();
    Block jumpBlock = mergeBlock;
    Block elseBlock = null;

    final PsiStatement elseBranch = statement.getElseBranch();
    if (elseBranch != null) {
      elseBlock = newBlock();
      jumpBlock = elseBlock;
    }

    addConditionalJumpStatement(statement.getCondition(), thenBlock, jumpBlock);

    currentBlock = thenBlock;
    statement.getThenBranch().accept(this);
    addUnconditionalJumpStatement(mergeBlock);

    if (elseBranch != null) {
      currentBlock = elseBlock;
      elseBranch.accept(this);
      addUnconditionalJumpStatement(mergeBlock);
    }

    currentBlock = mergeBlock;
  }

  private void visitLoop(@NotNull final PsiExpression condition, @NotNull final PsiStatement body, final boolean atLeastOnce) {
    final Block conditionBlock = newBlock();
    final Block bodyBlock = newBlock();
    final Block mergeBlock = newBlock();

    addUnconditionalJumpStatement(atLeastOnce ? bodyBlock : conditionBlock);

    currentBlock = conditionBlock;
    addConditionalJumpStatement(condition, bodyBlock, mergeBlock);

    currentBlock = bodyBlock;
    body.accept(this);
    addUnconditionalJumpStatement(conditionBlock);

    currentBlock = mergeBlock;
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final PsiExpression condition = statement.getCondition();
    if (condition == null) {
      return;
    }
    final PsiStatement body = statement.getBody();
    if (body == null) {
      return;
    }
    visitLoop(condition, body, false);
  }

  @Override
  public void visitDoWhileStatement(PsiDoWhileStatement statement) {
    final PsiExpression condition = statement.getCondition();
    if (condition == null) {
      return;
    }
    final PsiStatement body = statement.getBody();
    if (body == null) {
      return;
    }
    visitLoop(condition, body, true);
  }

  private void addStatements(@NotNull final PsiStatement statement) {
    if (statement instanceof PsiExpressionStatement) {
      addStatement(((PsiExpressionStatement)statement).getExpression().getText() + ";");
      return;
    }
    if (statement instanceof PsiExpressionListStatement) {
      for (PsiExpression expression : ((PsiExpressionListStatement)statement).getExpressionList().getExpressions()) {
        addStatement(expression.getText() + ";");
      }
    }
  }

  @Override
  public void visitForStatement(PsiForStatement statement) {
    final PsiStatement initialization = statement.getInitialization();
    if (initialization != null && !(initialization instanceof PsiEmptyStatement)) {
      addStatements(initialization);
    }

    final PsiExpression condition = statement.getCondition();
    final PsiStatement update = statement.getUpdate();

    final Block conditionBlock = condition != null ? newBlock() : null;
    final Block bodyBlock = newBlock();
    final Block updateBlock = update != null ? newBlock() : null;
    final Block mergeBlock = newBlock();

    final Block actualConditionBlock = conditionBlock != null ? conditionBlock : bodyBlock;
    final Block actualUpdateBlock = updateBlock != null ? updateBlock : actualConditionBlock;

    addUnconditionalJumpStatement(actualConditionBlock);

    if (conditionBlock != null) {
      currentBlock = conditionBlock;
      addConditionalJumpStatement(condition, bodyBlock, mergeBlock);
    }

    if (updateBlock != null) {
      currentBlock = updateBlock;
      addStatements(update);
      addUnconditionalJumpStatement(actualConditionBlock);
    }

    currentBlock = bodyBlock;
    statement.getBody().accept(this);
    addUnconditionalJumpStatement(actualUpdateBlock);

    currentBlock = mergeBlock;
  }

  @Override
  public void visitLambdaExpression(PsiLambdaExpression expression) {
  }

  @Override
  public void visitClass(PsiClass aClass) {
  }

  class Pair {
    private int id;
    @NotNull private PsiCodeBlock block;

    public Pair(final int id, @NotNull final PsiCodeBlock block) {
      this.id = id;
      this.block = block;
    }

    int getId() {
      return id;
    }

    @NotNull
    public PsiCodeBlock getBlock() {
      return block;
    }
  }

  List<Pair> getBlocks() {
    final List<Block> reachableBlocks = RemoveUnreachableBlocks.getInstance().apply(blocks);

    final List<Block> nonTrivialReachableBlocks = reachableBlocks
      .stream()
      .filter(Block::inlineIfTrivial)
      .collect(Collectors.toList());

    return nonTrivialReachableBlocks.stream().filter(block -> !block.isInlinable()).map(block -> {
      InlineVisitor inlineVisitor = new InlineVisitor(factory, nameManager);
      block.accept(inlineVisitor);
      return new Pair(block.getId(), inlineVisitor.getBlock());
    }).collect(Collectors.toList());
  }
}
