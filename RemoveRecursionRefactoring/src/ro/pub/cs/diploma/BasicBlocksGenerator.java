package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

class BasicBlocksGenerator extends JavaRecursiveElementVisitor {
  @Override
  public void visitLambdaExpression(PsiLambdaExpression expression) {
  }

  @Override
  public void visitClass(PsiClass aClass) {
  }

  class Pair {
    private final PsiCodeBlock block;
    private final int id;

    Pair(final PsiCodeBlock block, final int id) {
      this.block = block;
      this.id = id;
    }

    PsiCodeBlock getBlock() {
      return block;
    }

    int getId() {
      return id;
    }
  }

  private Pair currentPair;
  private PsiStatement currentStatement;
  private final PsiElementFactory factory;
  private int blockCounter;
  private final String frameClassName;
  private final String frameVarName;
  private final String blockFieldName;
  private final String stackVarName;
  private final PsiType returnType;
  private final String retVarName;
  private final Map<PsiStatement, Integer> breakJumps = new HashMap<>();
  private final Map<PsiStatement, Integer> continueJumps = new HashMap<>();
  private final Map<Integer, Pair> blocksMap = new HashMap<>();
  private final Set<Pair> theBlocks = new LinkedHashSet<>();

  private Pair newPair() {
    final Pair pair = new Pair(factory.createCodeBlock(), blockCounter++);
    blocksMap.put(pair.getId(), pair);
    return pair;
  }

  @NotNull
  private PsiStatement createStatement(@NotNull final String text) {
    return factory.createStatementFromText(text, null);
  }

  @Contract(pure = true)
  private static boolean breakOrReturnStatement(@NotNull final PsiStatement statement) {
    return statement instanceof PsiBreakStatement || statement instanceof PsiReturnStatement;
  }

  private boolean isJumpNecessary() {
    final PsiStatement[] statements = currentPair.getBlock().getStatements();
    return statements.length == 0 || !breakOrReturnStatement(statements[statements.length - 1]);
  }

  private boolean createJumpCommon(String indexExpression) {
    if (!isJumpNecessary()) {
      return false;
    }
    final PsiCodeBlock block = currentPair.getBlock();
    block.add(createStatement(frameVarName + "." + blockFieldName + " = " + indexExpression + ";"));
    block.add(createStatement("break;"));
    return true;
  }

  private void createJump(int index) {
    final boolean jumped = createJumpCommon(Integer.toString(index));
    if (jumped && theBlocks.contains(currentPair)) {
      theBlocks.add(blocksMap.get(index));
    }
  }

  private void createConditionalJump(PsiExpression condition, int thenIndex, int elseIndex) {
    final boolean jumped = createJumpCommon(condition.getText() + " ? " + thenIndex + " : " + elseIndex);
    if (jumped && theBlocks.contains(currentPair)) {
      theBlocks.add(blocksMap.get(thenIndex));
      theBlocks.add(blocksMap.get(elseIndex));
    }
  }

  BasicBlocksGenerator(final PsiElementFactory factory,
                       final String frameClassName,
                       final String frameVarName,
                       final String blockFieldName,
                       final String stackVarName,
                       final PsiType returnType,
                       final String retVarName) {
    this.factory = factory;
    currentPair = newPair();
    theBlocks.add(currentPair);
    this.frameClassName = frameClassName;
    this.frameVarName = frameVarName;
    this.blockFieldName = blockFieldName;
    this.stackVarName = stackVarName;
    this.returnType = returnType;
    this.retVarName = retVarName;
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    super.visitCodeBlock(block);

    final PsiStatement[] statements = block.getStatements();
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID.equals(returnType) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      currentPair.getBlock().add(createStatement("return;"));
    }
  }

  @Override
  public void visitDeclarationStatement(PsiDeclarationStatement statement) {
    currentStatement = (PsiStatement)currentPair.getBlock().add(statement);
    super.visitDeclarationStatement(statement);
  }

  @Override
  public void visitExpressionStatement(PsiExpressionStatement statement) {
    //        super.visitExpressionStatement(statement);
    currentStatement = (PsiStatement)currentPair.getBlock().add(statement);
    statement.getExpression().accept(this);
  }

  @Override
  public void visitIfStatement(PsiIfStatement statement) {
    final Pair thenPair = newPair();
    final Pair mergePair = newPair();
    int index = mergePair.getId();
    Pair elsePair = null;

    final PsiStatement elseBranch = statement.getElseBranch();
    if (elseBranch != null) {
      elsePair = newPair();
      index = elsePair.getId();
    }

    createConditionalJump(statement.getCondition(), thenPair.getId(), index);

    currentPair = thenPair;
    statement.getThenBranch().accept(this);
    createJump(mergePair.getId());

    if (elseBranch != null) {
      currentPair = elsePair;
      elseBranch.accept(this);
      createJump(mergePair.getId());
    }

    currentPair = mergePair;
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final Pair conditionPair = newPair();
    final Pair bodyPair = newPair();
    final Pair mergePair = newPair();

    breakJumps.put(statement, mergePair.getId());
    continueJumps.put(statement, conditionPair.getId());

    createJump(conditionPair.getId());

    currentPair = conditionPair;
    createConditionalJump(statement.getCondition(), bodyPair.getId(), mergePair.getId());

    currentPair = bodyPair;
    statement.getBody().accept(this);
    createJump(conditionPair.getId());

    currentPair = mergePair;
  }

  @Override
  public void visitDoWhileStatement(PsiDoWhileStatement statement) {
    final Pair conditionPair = newPair();
    final Pair bodyPair = newPair();
    final Pair mergePair = newPair();

    breakJumps.put(statement, mergePair.getId());
    continueJumps.put(statement, conditionPair.getId());

    createJump(bodyPair.getId());

    currentPair = bodyPair;
    statement.getBody().accept(this);
    createJump(conditionPair.getId());

    currentPair = conditionPair;
    createConditionalJump(statement.getCondition(), bodyPair.getId(), mergePair.getId());

    currentPair = mergePair;
  }

  @Override
  public void visitBlockStatement(PsiBlockStatement statement) {
    //        super.visitBlockStatement(statement);
    for (PsiStatement statement1 : statement.getCodeBlock().getStatements()) {
      statement1.accept(this);
    }
  }

  @Override
  public void visitReturnStatement(PsiReturnStatement statement) {
    //        super.visitReturnStatement(statement);
    currentPair.getBlock().add(statement);
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    //        super.visitMethodCallExpression(expression);
    final PsiMethod method = IterativeMethodGenerator.isRecursiveMethodCall(expression);
    if (method == null) {
      return;
    }

    final Pair newPair = newPair();

    currentPair.getBlock().add(IterativeMethodGenerator
                                 .createAddStatement(factory, frameClassName, stackVarName, expression.getArgumentList().getExpressions(),
                                                     PsiElement::getText));
    createJump(newPair.getId());

    currentStatement.delete();

    currentPair = newPair;

    final PsiElement parent = expression.getParent();
    if (parent instanceof PsiAssignmentExpression) {
      PsiAssignmentExpression assignment = (PsiAssignmentExpression)parent;
      currentPair.getBlock().add(createStatement(assignment.getLExpression().getText() + " = " + retVarName + ";"));
    }
  }

  @Override
  public void visitBreakStatement(PsiBreakStatement statement) {
    super.visitBreakStatement(statement);
    final PsiStatement exitedStatement = statement.findExitedStatement();
    if (exitedStatement == null) {
      return;
    }
    createJump(breakJumps.get(exitedStatement));
  }

  @Override
  public void visitContinueStatement(PsiContinueStatement statement) {
    super.visitContinueStatement(statement);
    final PsiStatement continuedStatement = statement.findContinuedStatement();
    if (continuedStatement == null) {
      return;
    }
    createJump(continueJumps.get(continuedStatement));
  }

  List<Pair> getBlocks() {
    return new ArrayList<>(theBlocks);
  }
}
