package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.psiutils.ExpressionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ro.pub.cs.diploma.ir.Block;
import ro.pub.cs.diploma.ir.NormalStatement;
import ro.pub.cs.diploma.ir.Statement;
import ro.pub.cs.diploma.ir.UnconditionalJumpStatement;

import java.util.*;

class BasicBlocksGenerator extends JavaRecursiveElementVisitor {
  @NotNull private final PsiMethod myMethod;
  @NotNull private final NameManager myNameManager;
  @NotNull private final PsiElementFactory myFactory;
  @NotNull private final Set<PsiStatement> myStatementsContainingRecursiveCalls;

  @NotNull private final List<Block> myBlocks = new ArrayList<>();
  @NotNull private final Map<PsiStatement, Block> myBreakTargets = new HashMap<>();
  @NotNull private final Map<PsiStatement, Block> myContinueTargets = new HashMap<>();

  @NotNull private Block myCurrentBlock;
  private int myCounter;

  BasicBlocksGenerator(@NotNull final PsiMethod method,
                       @NotNull final NameManager nameManager,
                       @NotNull final PsiElementFactory factory,
                       @NotNull final Set<PsiStatement> statementsContainingRecursiveCalls) {
    myMethod = method;
    myNameManager = nameManager;
    myFactory = factory;
    myStatementsContainingRecursiveCalls = statementsContainingRecursiveCalls;
    myCurrentBlock = newBlock();
  }

  private Block newBlock() {
    final Block block = new Block(myCounter++);
    myBlocks.add(block);
    return block;
  }

  private void addStatement(PsiStatement statement) {
    myCurrentBlock.add(statement);
  }

  private void addStatement(String text) {
    myCurrentBlock.add(myFactory.createStatementFromText(text, null));
  }

  private void addUnconditionalJumpStatement(@NotNull final Block block) {
    myCurrentBlock.addUnconditionalJump(Ref.create(block));
  }

  private void addConditionalJumpStatement(@NotNull final PsiExpression condition,
                                           @NotNull final Block thenBlock,
                                           @NotNull final Block jumpBlock) {
    myCurrentBlock.addConditionalJump(condition, Ref.create(thenBlock), Ref.create(jumpBlock));
  }

  private void addReturnStatement(@NotNull final PsiReturnStatement statement) {
    myCurrentBlock.addReturnStatement(statement);
  }

  private void processStatement(PsiStatement statement) {
    if (myStatementsContainingRecursiveCalls.contains(statement) ||
        statement instanceof PsiReturnStatement ||
        statement instanceof PsiBreakStatement ||
        statement instanceof PsiContinueStatement) {
      statement.accept(this);
      return;
    }

    final BreakContinueReplacerVisitor breakContinueReplacerVisitor = new BreakContinueReplacerVisitor(myBreakTargets, myContinueTargets,
                                                                                                       myFactory, myCurrentBlock);
    statement.accept(breakContinueReplacerVisitor);
    addStatement(statement);
  }

  @Override
  public void visitReturnStatement(PsiReturnStatement statement) {
    addReturnStatement(statement);
  }

  private void processStatement(@NotNull final PsiStatement statement,
                                @NotNull final PsiStatement targetStatement,
                                @NotNull final Map<PsiStatement, Block> targets) {
    final Block block = targets.get(targetStatement);
    if (block == null) {
      addStatement(statement);
      return;
    }
    addUnconditionalJumpStatement(block);
  }

  @Override
  public void visitBreakStatement(PsiBreakStatement statement) {
    final PsiStatement exitedStatement = statement.findExitedStatement();
    if (exitedStatement == null) {
      return;
    }
    processStatement(statement, exitedStatement, myBreakTargets);
  }

  @Override
  public void visitContinueStatement(PsiContinueStatement statement) {
    final PsiStatement continuedStatement = statement.findContinuedStatement();
    if (continuedStatement == null) {
      return;
    }
    processStatement(statement, continuedStatement, myContinueTargets);
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
    block.setDoNotInline(true);
    addUnconditionalJumpStatement(block);

    myCurrentBlock = block;

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

    myCurrentBlock = thenBlock;
    final PsiStatement thenBranch = statement.getThenBranch();
    if (thenBranch == null) {
      return;
    }
    thenBranch.accept(this);
    addUnconditionalJumpStatement(mergeBlock);

    if (elseBranch != null) {
      myCurrentBlock = elseBlock;
      elseBranch.accept(this);
      addUnconditionalJumpStatement(mergeBlock);
    }

    myCurrentBlock = mergeBlock;
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
    final Object expression = ExpressionUtils.computeConstantExpression(condition);
    final PsiExpression theCondition;
    if (expression instanceof Boolean && expression.equals(Boolean.TRUE)) {
      theCondition = null;
    }
    else {
      theCondition = condition;
    }

    final Block conditionBlock = theCondition != null ? newBlock() : null;
    final Block bodyBlock = newBlock();
    final Block updateBlock = update != null ? newBlock() : null;
    final Block mergeBlock = newBlock();

    final Block actualConditionBlock = conditionBlock != null ? conditionBlock : bodyBlock;
    final Block actualUpdateBlock = updateBlock != null ? updateBlock : actualConditionBlock;

    myBreakTargets.put(statement, mergeBlock);
    myContinueTargets.put(statement, actualUpdateBlock);

    addUnconditionalJumpStatement(atLeastOnce ? bodyBlock : actualConditionBlock);

    if (conditionBlock != null) {
      myCurrentBlock = conditionBlock;
      addConditionalJumpStatement(theCondition, bodyBlock, mergeBlock);
    }

    if (updateBlock != null) {
      myCurrentBlock = updateBlock;
      addStatements(update);
      addUnconditionalJumpStatement(actualConditionBlock);
    }

    myCurrentBlock = bodyBlock;
    final PsiStatement body = statement.getBody();
    if (body == null) {
      return;
    }
    body.accept(this);
    addUnconditionalJumpStatement(actualUpdateBlock);

    myCurrentBlock = mergeBlock;
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
  public void visitSwitchStatement(PsiSwitchStatement statement) {
    final Block mergeBlock = newBlock();
    myBreakTargets.put(statement, mergeBlock);

    final PsiCodeBlock body = statement.getBody();
    if (body == null) {
      return;
    }
    final List<Statement> statements = new ArrayList<>();
    final Block oldCurrentBlock = myCurrentBlock;
    for (PsiStatement psiStatement : body.getStatements()) {
      if (psiStatement instanceof PsiSwitchLabelStatement) {
        statements.add(new NormalStatement(psiStatement));
        continue;
      }
      if (psiStatement instanceof PsiBlockStatement) {
        final Block newBlock = newBlock();
        statements.add(new UnconditionalJumpStatement(Ref.create(newBlock)));

        myCurrentBlock = newBlock;
        psiStatement.accept(this);
      }
    }

    addUnconditionalJumpStatement(mergeBlock);

    myCurrentBlock = oldCurrentBlock;
    final PsiExpression expression = statement.getExpression();
    if (expression == null) {
      return;
    }
    myCurrentBlock.addSwitchStatement(expression, statements);

    myCurrentBlock = mergeBlock;
  }

  @Override
  public void visitLambdaExpression(PsiLambdaExpression expression) {
  }

  @Override
  public void visitClass(PsiClass aClass) {
  }

  @NotNull
  List<Block> getBlocks() {
    return myBlocks;
  }
}
