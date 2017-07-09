package ro.pub.cs.diploma;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
      currentPair.getBlock().add(factory.createStatementFromText("return;", null));
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

  private Pair newPair() {
    final Pair pair = new Pair(factory.createCodeBlock(), blockCounter++);
    blocks.add(pair);
    return pair;
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
    addBreakToMergeBlock(mergePair.getId());

    if (elseBranch != null) {
      currentPair = elsePair;
      elseBranch.accept(this);
      addBreakToMergeBlock(mergePair.getId());
    }

    currentPair = mergePair;
  }

  private void createConditionalJump(PsiExpression condition, int thenIndex, int elseIndex) {
    final PsiCodeBlock block = currentPair.getBlock();
    block.add(factory.createStatementFromText(
      "context.section = " + condition.getText() + "? " + thenIndex + " : " + elseIndex + ";", null));
    block.add(factory.createStatementFromText("break;", null));
  }

  private void createJump(int index) {
    final PsiCodeBlock block = currentPair.getBlock();
    block.add(factory.createStatementFromText("context.section = " + index + ";", null));
    block.add(factory.createStatementFromText("break;", null));
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final Pair conditionPair = newPair();
    final Pair bodyPair = newPair();
    final Pair mergePair = newPair();

    createJump(conditionPair.getId());

    currentPair = conditionPair;
    createConditionalJump(statement.getCondition(), bodyPair.getId(), mergePair.getId());

    currentPair = bodyPair;
    statement.getBody().accept(this);
    addBreakToMergeBlock(conditionPair.getId());

    currentPair = mergePair;
  }

  private void addBreakToMergeBlock(int index) {
    final PsiStatement[] statements = currentPair.getBlock().getStatements();
    if (statements.length == 0 || !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      createJump(index);
    }
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

      List<PsiStatement> statements1 = new ArrayList<>();
      statements1.add(factory.createStatementFromText("context.section = " + newPair.getId() + ";", null));
      final String s = Arrays.stream(expression.getArgumentList().getExpressions())
        .map(PsiElement::getText).collect(Collectors.joining(","));
      statements1.add(factory.createStatementFromText("stack.push(new " +
                                                      contextClassName + "(" + s + "));", null));
      statements1.add(factory.createStatementFromText("break;", null));
      for (PsiStatement statement : statements1) {
        currentPair.getBlock().add(statement);
      }
      currentStatement.delete();

      currentPair = newPair;

      final PsiElement parent = expression.getParent();
      if (parent instanceof PsiAssignmentExpression) {
        PsiAssignmentExpression assignment = (PsiAssignmentExpression)parent;
        currentPair.getBlock().add(factory.createStatementFromText(
          assignment.getLExpression().getText() + " = ret;", null));
      }
    }
  }

  List<Pair> getBlocks() {
    return blocks;
  }
}
