package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import ro.pub.cs.diploma.ir.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class BasicBlocksGenerator2 extends JavaRecursiveElementVisitor {
  private final PsiElementFactory factory;
  private final PsiMethod method;
  private final String frameClassName;
  private final String frameVarName;
  private final String blockFieldName;
  private final String stackVarName;
  private final PsiType returnType;
  private final String retVarName;
  private final List<Block> blocks = new ArrayList<>();

  private Block currentBlock;
  private int counter;

  BasicBlocksGenerator2(final PsiElementFactory factory,
                        final PsiMethod method,
                        final String frameClassName,
                        final String frameVarName,
                        final String blockFieldName,
                        final String stackVarName,
                        final PsiType returnType,
                        final String retVarName) {
    this.factory = factory;
    this.method = method;
    currentBlock = newBlock();
    this.frameClassName = frameClassName;
    this.frameVarName = frameVarName;
    this.blockFieldName = blockFieldName;
    this.stackVarName = stackVarName;
    this.returnType = returnType;
    this.retVarName = retVarName;
  }

  private Block newBlock() {
    final Block block = new Block(counter++);
    blocks.add(block);
    return block;
  }

  private void addStatement(String text) {
    currentBlock.add(factory.createStatementFromText(text, null));
  }

  private void addUnconditionalJumpStatement(Block block) {
    if (currentBlock.isFinished())
      return;
    final Ref<Block> blockRef = Ref.create(block);
    currentBlock.add(new UnconditionalJumpStatement(blockRef));
    block.addReference(blockRef);
  }

  private void addConditionalJumpStatement(PsiExpression condition, Block thenBlock, Block jumpBlock) {
    if (currentBlock.isFinished())
      return;
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
    if (Visitors.containsRecursiveCalls(statement, method) || statement instanceof PsiReturnStatement) {
      statement.accept(this);
    }
    else {
      currentBlock.add(statement);
    }
  }

  @Override
  public void visitReturnStatement(PsiReturnStatement statement) {
    super.visitReturnStatement(statement);
    addReturnStatement(statement);
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    Arrays.stream(block.getStatements()).forEach(this::processStatement);

    final PsiStatement[] statements = block.getStatements();
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID.equals(returnType) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      addReturnStatement((PsiReturnStatement)factory.createStatementFromText("return;", null));
    }
  }

  @Override
  public void visitBlockStatement(PsiBlockStatement blockStatement) {
    Arrays.stream(blockStatement.getCodeBlock().getStatements()).forEach(this::processStatement);
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    currentBlock.add(IterativeMethodGenerator.createPushStatement(
      factory, frameClassName, stackVarName, expression.getArgumentList().getExpressions(), PsiElement::getText));

    final Block block = newBlock();
    block.setAfterRecursiveCall(true);
    addUnconditionalJumpStatement(block);

    currentBlock = block;

    final PsiElement parent = expression.getParent();
    if (parent instanceof PsiAssignmentExpression) {
      PsiAssignmentExpression assignment = (PsiAssignmentExpression)parent;
      addStatement(assignment.getLExpression().getText() + " = " + retVarName + ";");
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

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final Block conditionBlock = newBlock();
    final Block bodyBlock = newBlock();
    final Block mergeBlock = newBlock();

    addUnconditionalJumpStatement(conditionBlock);

    currentBlock = conditionBlock;
    addConditionalJumpStatement(statement.getCondition(), bodyBlock, mergeBlock);

    currentBlock = bodyBlock;
    statement.getBody().accept(this);
    addUnconditionalJumpStatement(conditionBlock);

    currentBlock = mergeBlock;
  }

  @Override
  public void visitDoWhileStatement(PsiDoWhileStatement statement) {
    final Block conditionBlock = newBlock();
    final Block bodyBlock = newBlock();
    final Block mergeBlock = newBlock();

    addUnconditionalJumpStatement(bodyBlock);

    currentBlock = bodyBlock;
    statement.getBody().accept(this);
    addUnconditionalJumpStatement(conditionBlock);

    currentBlock = conditionBlock;
    addConditionalJumpStatement(statement.getCondition(), bodyBlock, mergeBlock);

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
    private PsiCodeBlock block;

    public Pair(int id, PsiCodeBlock block) {
      this.id = id;
      this.block = block;
    }

    public int getId() {
      return id;
    }

    public PsiCodeBlock getBlock() {
      return block;
    }
  }

  List<Pair> getBlocks() {
    final List<Block> nonTrivialReachableBlocks = blocks.stream().
      filter(Block::isReachable).filter(Block::inlineIfTrivial).collect(Collectors.toList());

    for (Block block : nonTrivialReachableBlocks) {
      block.setInline();
    }

    List<Pair> pairs = new ArrayList<>();
    for (Block block : nonTrivialReachableBlocks) {
      if (block.isInline())
        continue;
      InlineVisitor inlineVisitor = new InlineVisitor(factory, frameVarName, blockFieldName);
      block.accept(inlineVisitor);
      pairs.add(new Pair(block.getId(), inlineVisitor.getBlock()));
    }

    return pairs;
  }
}
