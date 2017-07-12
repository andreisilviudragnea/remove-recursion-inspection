package ro.pub.cs.diploma;

import com.intellij.psi.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

class BasicBlocksGenerator extends JavaRecursiveElementVisitor {
  class Pair {
    private PsiCodeBlock block;
    private int id;

    Pair(PsiCodeBlock block, int id) {
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

  private List<Pair> blocks = new ArrayList<>();
  private Pair currentPair;
  private PsiStatement currentStatement;
  private PsiElementFactory factory;
  private int blockCounter;
  private String methodName;
  private String contextClassName;
  private PsiType returnType;
  private Map<PsiStatement, Integer> breakJumps = new HashMap<>();

  private Pair newPair() {
    final Pair pair = new Pair(factory.createCodeBlock(), blockCounter++);
    blocks.add(pair);
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

  private void createJump(int index) {
    if (!isJumpNecessary()) {
      return;
    }
    final PsiCodeBlock block = currentPair.getBlock();
    block.add(createStatement("context.section = " + index + ";"));
    block.add(createStatement("break;"));
  }

  private void createConditionalJump(PsiExpression condition, int thenIndex, int elseIndex) {
    if (!isJumpNecessary()) {
      return;
    }
    final PsiCodeBlock block = currentPair.getBlock();
    block.add(createStatement("context.section = " + condition.getText() + "? " + thenIndex + " : " + elseIndex + ";"));
    block.add(createStatement("break;"));
  }

  BasicBlocksGenerator(PsiElementFactory factory, String methodName, String contextClassName, PsiType returnType) {
    this.factory = factory;
    currentPair = newPair();
    this.methodName = methodName;
    this.contextClassName = contextClassName;
    this.returnType = returnType;
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

    createJump(conditionPair.getId());

    currentPair = conditionPair;
    createConditionalJump(statement.getCondition(), bodyPair.getId(), mergePair.getId());

    currentPair = bodyPair;
    statement.getBody().accept(this);
    createJump(conditionPair.getId());

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
    if (expression.getMethodExpression().getReferenceName().equals(methodName)) {
      final Pair newPair = newPair();

      final PsiCodeBlock block = currentPair.getBlock();
      block.add(createStatement("context.section = " + newPair.getId() + ";"));
      final String s = Arrays.stream(expression.getArgumentList().getExpressions())
        .map(PsiElement::getText).collect(Collectors.joining(","));
      block.add(createStatement("stack.add(new " + contextClassName + "(" + s + "));"));
      block.add(createStatement("break;"));

      currentStatement.delete();

      currentPair = newPair;

      final PsiElement parent = expression.getParent();
      if (parent instanceof PsiAssignmentExpression) {
        PsiAssignmentExpression assignment = (PsiAssignmentExpression)parent;
        currentPair.getBlock().add(createStatement(assignment.getLExpression().getText() + " = ret;"));
      }
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

  List<Pair> getBlocks() {
    return blocks;
  }
}
