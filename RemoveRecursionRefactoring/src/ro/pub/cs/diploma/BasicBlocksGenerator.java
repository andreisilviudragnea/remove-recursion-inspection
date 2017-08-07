package ro.pub.cs.diploma;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.pub.cs.diploma.ir.*;
import ro.pub.cs.diploma.passes.RemoveUnreachableBlocks;

import java.util.*;
import java.util.stream.Collectors;

class BasicBlocksGenerator extends JavaRecursiveElementVisitor {
  @NotNull private final PsiMethod myMethod;
  @NotNull private final NameManager myNameManager;
  @NotNull private final PsiElementFactory myFactory;

  @NotNull private final List<Block> myBlocks = new ArrayList<>();
  @NotNull private final Map<PsiStatement, Block> myBreakTargets = new HashMap<>();
  @NotNull private final Map<PsiStatement, Block> myContinueTargets = new HashMap<>();

  @NotNull private Block currentBlock;
  private int counter;

  BasicBlocksGenerator(@NotNull final PsiMethod method, @NotNull final NameManager nameManager, @NotNull final PsiElementFactory factory) {
    myMethod = method;
    myNameManager = nameManager;
    myFactory = factory;
    currentBlock = newBlock();
  }

  private Block newBlock() {
    final Block block = new Block(counter++);
    myBlocks.add(block);
    return block;
  }

  @NotNull
  private PsiStatement statement(String text) {
    return myFactory.createStatementFromText(text, null);
  }

  private void addStatement(PsiStatement statement) {
    currentBlock.add(statement);
  }

  private void addStatement(String text) {
    currentBlock.add(statement(text));
  }

  private void addUnconditionalJumpStatement(@NotNull final Block block) {
    if (currentBlock.isFinished()) {
      return;
    }
    final Ref<Block> blockRef = Ref.create(block);
    currentBlock.add(new UnconditionalJumpStatement(blockRef));
    block.addReference(blockRef);
  }

  private void addConditionalJumpStatement(@NotNull final PsiExpression condition,
                                           @NotNull final Block thenBlock,
                                           @NotNull final Block jumpBlock) {
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
    if (RecursionUtil.containsRecursiveCalls(statement, myMethod)) {
      statement.accept(this);
      return;
    }
    if (statement instanceof PsiReturnStatement) {
      addReturnStatement((PsiReturnStatement)statement);
      return;
    }
    if (statement instanceof PsiBreakStatement) {
      final PsiStatement exitedStatement = ((PsiBreakStatement)statement).findExitedStatement();
      if (exitedStatement == null) {
        return;
      }
      final Block block = myBreakTargets.get(exitedStatement);
      if (block == null) {
        addStatement(statement);
        return;
      }
      addUnconditionalJumpStatement(block);
      return;
    }
    if (statement instanceof PsiContinueStatement) {
      final PsiStatement continuedStatement = ((PsiContinueStatement)statement).findContinuedStatement();
      if (continuedStatement == null) {
        return;
      }
      final Block block = myContinueTargets.get(continuedStatement);
      if (block == null) {
        addStatement(statement);
        return;
      }
      addUnconditionalJumpStatement(block);
      return;
    }
    final BreakContinueReplacerVisitor breakContinueReplacerVisitor = new BreakContinueReplacerVisitor(myBreakTargets, myContinueTargets,
                                                                                                       myFactory);
    statement.accept(breakContinueReplacerVisitor);
    addStatement(statement);
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    Arrays.stream(block.getStatements()).forEach(this::processStatement);

    final PsiStatement[] statements = block.getStatements();
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID.equals(myMethod.getReturnType()) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      addReturnStatement((PsiReturnStatement)myFactory.createStatementFromText("return;", null));
    }
  }

  @Override
  public void visitBlockStatement(PsiBlockStatement blockStatement) {
    Arrays.stream(blockStatement.getCodeBlock().getStatements()).forEach(this::processStatement);
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    addStatement(Util.createPushStatement(myFactory, myNameManager.getFrameClassName(), myNameManager.getStackVarName(),
                                          expression.getArgumentList().getExpressions(), PsiElement::getText));

    final Block block = newBlock();
    block.setAfterRecursiveCall(true);
    addUnconditionalJumpStatement(block);

    currentBlock = block;

    final PsiType returnType = myMethod.getReturnType();
    if (returnType == null) {
      return;
    }
    if (!Util.isVoid(returnType)) {
      final PsiStatement parent = PsiTreeUtil.getParentOfType(expression, PsiStatement.class, true);
      expression.replace(myFactory.createExpressionFromText(myNameManager.getRetVarName(), null));
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

    final PsiExpression condition = statement.getCondition();
    if (condition == null) {
      return;
    }
    addConditionalJumpStatement(condition, thenBlock, jumpBlock);

    currentBlock = thenBlock;
    final PsiStatement thenBranch = statement.getThenBranch();
    if (thenBranch == null) {
      return;
    }
    thenBranch.accept(this);
    addUnconditionalJumpStatement(mergeBlock);

    if (elseBranch != null) {
      currentBlock = elseBlock;
      elseBranch.accept(this);
      addUnconditionalJumpStatement(mergeBlock);
    }

    currentBlock = mergeBlock;
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

  private void visitLoop(@Nullable final PsiExpression condition,
                         @Nullable final PsiStatement update,
                         @NotNull final PsiLoopStatement statement,
                         final boolean atLeastOnce) {
    final Block conditionBlock = condition != null ? newBlock() : null;
    final Block bodyBlock = newBlock();
    final Block updateBlock = update != null ? newBlock() : null;
    final Block mergeBlock = newBlock();

    final Block actualConditionBlock = conditionBlock != null ? conditionBlock : bodyBlock;
    final Block actualUpdateBlock = updateBlock != null ? updateBlock : actualConditionBlock;

    myBreakTargets.put(statement, mergeBlock);
    myContinueTargets.put(statement, actualUpdateBlock);

    addUnconditionalJumpStatement(atLeastOnce ? bodyBlock : actualConditionBlock);

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
    final PsiStatement body = statement.getBody();
    if (body == null) {
      return;
    }
    body.accept(this);
    addUnconditionalJumpStatement(actualUpdateBlock);

    currentBlock = mergeBlock;
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final PsiExpression condition = statement.getCondition();
    if (condition == null) {
      return;
    }
    visitLoop(condition, null, statement, false);
  }

  @Override
  public void visitDoWhileStatement(PsiDoWhileStatement statement) {
    final PsiExpression condition = statement.getCondition();
    if (condition == null) {
      return;
    }
    visitLoop(condition, null, statement, true);
  }

  @Override
  public void visitForStatement(PsiForStatement statement) {
    final PsiStatement initialization = statement.getInitialization();
    if (initialization != null && !(initialization instanceof PsiEmptyStatement)) {
      addStatements(initialization);
    }

    visitLoop(statement.getCondition(), statement.getUpdate(), statement, false);
  }

  @Override
  public void visitLambdaExpression(PsiLambdaExpression expression) {
  }

  @Override
  public void visitClass(PsiClass aClass) {
  }

  List<Pair<Integer, PsiCodeBlock>> getBlocks() {
    final List<Block> reachableBlocks = RemoveUnreachableBlocks.getInstance().apply(myBlocks);

    final List<Block> nonTrivialReachableBlocks = reachableBlocks
      .stream()
      .filter(Block::inlineIfTrivial)
      .collect(Collectors.toList());

    return nonTrivialReachableBlocks.stream().filter(block -> !block.isInlinable()).map(block -> {
      InlineVisitor inlineVisitor = new InlineVisitor(myFactory, myNameManager);
      block.accept(inlineVisitor);
      return Pair.create(block.getId(), inlineVisitor.getBlock());
    }).collect(Collectors.toList());
  }
}
