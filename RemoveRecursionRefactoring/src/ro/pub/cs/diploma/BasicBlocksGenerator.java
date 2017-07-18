package ro.pub.cs.diploma;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
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
    private JumpBase jump;
    private final List<Ref<Integer>> references = new ArrayList<>();
    private boolean isFinished = false;

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

    void setJump(JumpBase jump) {
      this.jump = jump;
    }

    void addReference(Ref<Integer> reference) {
      references.add(reference);
    }
  }

  private class JumpBase {

  }

  private class Jump extends JumpBase {
    private Ref<Integer> ref;

    private Jump(Ref<Integer> ref) {
      this.ref = ref;
    }

    @Override
    public String toString() {
      return ref.toString();
    }
  }

  private class ConditionalJump extends JumpBase {
    private String condition;
    private Ref<Integer> ref1;
    private Ref<Integer> ref2;

    private ConditionalJump(String condition, Ref<Integer> ref1, Ref<Integer> ref2) {
      this.condition = condition;
      this.ref1 = ref1;
      this.ref2 = ref2;
    }

    @Override
    public String toString() {
      return condition + " ? " + ref1 + " : " + ref2;
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
  private final String switchLabelName;
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

  private void createJump(int index) {
    if (currentPair.isFinished) {
      return;
    }
    currentPair.isFinished = true;
    if (theBlocks.contains(currentPair)) {
      final Ref<Integer> ref = new Ref<>(index);
      currentPair.setJump(new Jump(ref));
      final Pair pair = blocksMap.get(index);
      pair.addReference(ref);
      theBlocks.add(pair);
    }
  }

  private void createConditionalJump(PsiExpression condition, int thenIndex, int elseIndex) {
    if (currentPair.isFinished) {
      return;
    }
    currentPair.isFinished = true;
    if (theBlocks.contains(currentPair)) {
      final Ref<Integer> ref1 = new Ref<>(thenIndex);
      final Ref<Integer> ref2 = new Ref<>(elseIndex);
      currentPair.setJump(new ConditionalJump(condition.getText(), ref1, ref2));
      final Pair thenPair = blocksMap.get(thenIndex);
      thenPair.addReference(ref1);
      theBlocks.add(thenPair);
      final Pair elsePair = blocksMap.get(elseIndex);
      elsePair.addReference(ref2);
      theBlocks.add(elsePair);
    }
  }

  BasicBlocksGenerator(final PsiElementFactory factory,
                       final String frameClassName,
                       final String frameVarName,
                       final String blockFieldName,
                       final String stackVarName,
                       final PsiType returnType,
                       final String retVarName,
                       final String switchLabelName) {
    this.factory = factory;
    currentPair = newPair();
    theBlocks.add(currentPair);
    this.frameClassName = frameClassName;
    this.frameVarName = frameVarName;
    this.blockFieldName = blockFieldName;
    this.stackVarName = stackVarName;
    this.returnType = returnType;
    this.retVarName = retVarName;
    this.switchLabelName = switchLabelName;
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    Arrays.stream(block.getStatements()).forEach(this::processStatement);

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
  public void visitBlockStatement(PsiBlockStatement blockStatement) {
    Arrays.stream(blockStatement.getCodeBlock().getStatements()).forEach(this::processStatement);
  }

  private void processStatement(PsiStatement statement) {
    if (Visitors.containsRecursiveCalls(statement)) {
      statement.accept(this);
    } else {
      currentPair.getBlock().add(statement);
    }
    if (statement instanceof PsiReturnStatement) {
      currentPair.isFinished = true;
    }
  }

  @Override
  public void visitReturnStatement(PsiReturnStatement statement) {
    currentPair.getBlock().add(statement);
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    final PsiMethod method = IterativeMethodGenerator.isRecursiveMethodCall(expression);
    if (method == null) {
      return;
    }

    final Pair newPair = newPair();

    currentPair.getBlock().add(IterativeMethodGenerator
                                 .createPushStatement(factory, frameClassName, stackVarName, expression.getArgumentList().getExpressions(),
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
    final List<Pair> pairs = new ArrayList<>();

    // Remove trivial blocks (which contain only an unconditional jump)
    for (Pair theBlock : theBlocks) {
      if (theBlock.block.getStatements().length == 0 && theBlock.jump instanceof Jump) {
        Jump jump = (Jump)theBlock.jump;
        for (Ref<Integer> reference : theBlock.references) {
          reference.set(jump.ref.get());
        }
        continue;
      }
      pairs.add(theBlock);
    }

    for (Pair pair : pairs) {
      if (pair.jump != null) {
        pair.block.add(createStatement(frameVarName + "." + blockFieldName + " = " + pair.jump.toString() + ";"));
        pair.block.add(createStatement("break " + switchLabelName + ";"));
      }
    }

    return pairs;
  }
}
