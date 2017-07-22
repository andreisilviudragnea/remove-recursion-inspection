package ro.pub.cs.diploma;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class NewBlocksGenerator extends JavaRecursiveElementVisitor {
  @Override
  public void visitLambdaExpression(PsiLambdaExpression expression) {
  }

  @Override
  public void visitClass(PsiClass aClass) {
  }

  class Pair {
    private final PsiCodeBlock block;
    private final int id;
    private boolean isFinished;

    Pair(final int id) {
      block = factory.createCodeBlock();
      this.id = id;
    }

    PsiCodeBlock getBlock() {
      return block;
    }

    int getId() {
      return id;
    }

    void addStatement(String text) {
      block.add(factory.createStatementFromText(text, null));
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
  private final List<Pair> blocks = new ArrayList<>();

  private Pair newDetachedPair() {
    return new Pair(-1);
  }

  private Pair newPair() {
    final Pair pair = new Pair(blockCounter++);
    blocks.add(pair);
    return pair;
  }

  private void createJumpBase(String expression) {
    if (currentPair.isFinished) {
      return;
    }
    currentPair.addStatement(frameVarName + "." + blockFieldName + " = " + expression + ";");
    currentPair.addStatement("break;");
  }

  private void createJump(Pair pair) {
    createJumpBase(String.valueOf(pair.id));
  }

  NewBlocksGenerator(final PsiElementFactory factory,
                     final String frameClassName,
                     final String frameVarName,
                     final String blockFieldName,
                     final String stackVarName,
                     final PsiType returnType,
                     final String retVarName) {
    this.factory = factory;
    currentPair = newPair();
    this.frameClassName = frameClassName;
    this.frameVarName = frameVarName;
    this.blockFieldName = blockFieldName;
    this.stackVarName = stackVarName;
    this.returnType = returnType;
    this.retVarName = retVarName;
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    Arrays.stream(block.getStatements()).forEach(this::processStatement);

    final PsiStatement[] statements = block.getStatements();
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID.equals(returnType) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      currentPair.addStatement("return;");
    }
  }

  @Override
  public void visitExpressionStatement(PsiExpressionStatement statement) {
    currentStatement = (PsiStatement)currentPair.getBlock().add(statement);
    statement.getExpression().accept(this);
  }

  @Override
  public void visitIfStatement(PsiIfStatement statement) {
    final Pair thenPair = newDetachedPair();
    final Pair mergePair = newPair();
    Pair elsePair = null;

    final PsiStatement elseBranch = statement.getElseBranch();
    if (elseBranch != null) {
      elsePair = newDetachedPair();
    }

    final Pair ifPair = currentPair;

    currentPair = thenPair;
    statement.getThenBranch().accept(this);
    createJump(mergePair);

    if (elseBranch != null) {
      currentPair = elsePair;
      elseBranch.accept(this);
      createJump(mergePair);
    }

    ifPair.addStatement("if(" +
                        statement.getCondition().getText() +
                        ")" +
                        thenPair.block.getText() +
                        (elsePair != null ? ("else" + elsePair.block.getText()) : ""));

    currentPair = mergePair;
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final Pair conditionPair = newPair();
    final Pair bodyPair = newDetachedPair();
    final Pair mergePair = newPair();

    createJump(conditionPair);

    currentPair = bodyPair;
    statement.getBody().accept(this);
    createJump(conditionPair);

    conditionPair.addStatement("if(" +
                               statement.getCondition().getText() +
                               ")" +
                               bodyPair.block.getText() +
                               "else {" + frameVarName + "." + blockFieldName + " = " + mergePair.id + ";\nbreak;}");

    currentPair = mergePair;
  }

  @Override
  public void visitBlockStatement(PsiBlockStatement blockStatement) {
    Arrays.stream(blockStatement.getCodeBlock().getStatements()).forEach(this::processStatement);
  }

  private void processStatement(PsiStatement statement) {
    if (Visitors.containsRecursiveCalls(statement)) {
      statement.accept(this);
    }
    else {
      currentPair.block.add(statement);
    }
    if (statement instanceof PsiReturnStatement) {
      currentPair.isFinished = true;
    }
  }

  @Override
  public void visitReturnStatement(PsiReturnStatement statement) {
    currentPair.block.add(statement);
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    final PsiMethod method = IterativeMethodGenerator.isRecursiveMethodCall(expression);
    if (method == null) {
      return;
    }

    final Pair newPair = newPair();

    currentPair.block.add(IterativeMethodGenerator
                            .createPushStatement(factory, frameClassName, stackVarName, expression.getArgumentList().getExpressions(),
                                                 PsiElement::getText));
    createJump(newPair);

    currentStatement.delete();

    currentPair = newPair;

    final PsiElement parent = expression.getParent();
    if (parent instanceof PsiAssignmentExpression) {
      PsiAssignmentExpression assignment = (PsiAssignmentExpression)parent;
      currentPair.addStatement(assignment.getLExpression().getText() + " = " + retVarName + ";");
    }
  }

  List<Pair> getBlocks() {
    return blocks;
  }
}
