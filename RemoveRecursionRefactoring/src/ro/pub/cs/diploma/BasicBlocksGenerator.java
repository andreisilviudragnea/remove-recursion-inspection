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

    void terminate() {
      jump.terminatePair(this);
    }
  }

  private abstract class JumpBase {
    abstract void terminatePair(Pair pair);
  }

  private class Jump extends JumpBase {
    private Ref<Integer> ref;

    private Jump(Ref<Integer> ref) {
      this.ref = ref;
    }

    @Override
    void terminatePair(Pair pair) {
      pair.block.add(createStatement(frameVarName + "." + blockFieldName + " = " + ref + ";"));
      pair.block.add(createStatement("break;"));
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
    void terminatePair(Pair pair) {
      pair.block.add(createStatement("if (" + condition + ") {" +
                                     frameVarName + "." + blockFieldName + " = " + ref1 + ";break;} else {" +
                                     frameVarName + "." + blockFieldName + "=" + ref2 + ";break;}"));
    }
  }

  private Pair currentPair;
  private final PsiElementFactory factory;
  private final PsiMethod method;
  private int blockCounter;
  private final String frameClassName;
  private final String frameVarName;
  private final String blockFieldName;
  private final String stackVarName;
  private final PsiType returnType;
  private final String retVarName;
  private final Map<PsiStatement, Pair> breakJumps = new HashMap<>();
  private final Map<PsiStatement, Pair> continueJumps = new HashMap<>();
  private final Set<Pair> reachableBlocks = new LinkedHashSet<>();

  private Pair newPair() {
    return new Pair(factory.createCodeBlock(), blockCounter++);
  }

  @NotNull
  private PsiStatement createStatement(@NotNull final String text) {
    return factory.createStatementFromText(text, null);
  }

  private void createJump(Pair pair) {
    if (currentPair.isFinished) {
      return;
    }
    currentPair.isFinished = true;
    if (reachableBlocks.contains(currentPair)) {
      final Ref<Integer> ref = new Ref<>(pair.id);
      currentPair.setJump(new Jump(ref));
      pair.addReference(ref);
      reachableBlocks.add(pair);
    }
  }

  private void createConditionalJump(PsiExpression condition, Pair thenPair, Pair elsePair) {
    if (currentPair.isFinished) {
      return;
    }
    currentPair.isFinished = true;
    if (reachableBlocks.contains(currentPair)) {
      final Ref<Integer> ref1 = new Ref<>(thenPair.id);
      final Ref<Integer> ref2 = new Ref<>(elsePair.id);
      currentPair.setJump(new ConditionalJump(condition.getText(), ref1, ref2));
      thenPair.addReference(ref1);
      reachableBlocks.add(thenPair);
      elsePair.addReference(ref2);
      reachableBlocks.add(elsePair);
    }
  }

  BasicBlocksGenerator(final PsiElementFactory factory,
                       final PsiMethod method,
                       final String frameClassName,
                       final String frameVarName,
                       final String blockFieldName,
                       final String stackVarName,
                       final PsiType returnType,
                       final String retVarName) {
    this.factory = factory;
    this.method = method;
    currentPair = newPair();
    reachableBlocks.add(currentPair);
    this.frameClassName = frameClassName;
    this.frameVarName = frameVarName;
    this.blockFieldName = blockFieldName;
    this.stackVarName = stackVarName;
    this.returnType = returnType;
    this.retVarName = retVarName;
  }


  private void processStatement(PsiStatement statement) {
    if (RecursionUtil.containsRecursiveCalls(statement, method)) {
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
  public void visitBlockStatement(PsiBlockStatement blockStatement) {
    Arrays.stream(blockStatement.getCodeBlock().getStatements()).forEach(this::processStatement);
  }

  @Override
  public void visitCodeBlock(PsiCodeBlock block) {
    Arrays.stream(block.getStatements()).forEach(this::processStatement);

    final PsiStatement[] statements = block.getStatements();
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID.equals(returnType) && !(statements[statements.length - 1] instanceof PsiReturnStatement)) {
      currentPair.block.add(createStatement("return;"));
    }
  }

  @Override
  public void visitIfStatement(PsiIfStatement statement) {
    final Pair thenPair = newPair();
    final Pair mergePair = newPair();
    Pair jumpPair = mergePair;
    Pair elsePair = null;

    final PsiStatement elseBranch = statement.getElseBranch();
    if (elseBranch != null) {
      elsePair = newPair();
      jumpPair = elsePair;
    }

    createConditionalJump(statement.getCondition(), thenPair, jumpPair);

    currentPair = thenPair;
    statement.getThenBranch().accept(this);
    createJump(mergePair);

    if (elseBranch != null) {
      currentPair = elsePair;
      elseBranch.accept(this);
      createJump(mergePair);
    }

    currentPair = mergePair;
  }

  @Override
  public void visitWhileStatement(PsiWhileStatement statement) {
    final Pair conditionPair = newPair();
    final Pair bodyPair = newPair();
    final Pair mergePair = newPair();

    breakJumps.put(statement, mergePair);
    continueJumps.put(statement, conditionPair);

    createJump(conditionPair);

    currentPair = conditionPair;
    createConditionalJump(statement.getCondition(), bodyPair, mergePair);

    currentPair = bodyPair;
    statement.getBody().accept(this);
    createJump(conditionPair);

    currentPair = mergePair;
  }

  @Override
  public void visitDoWhileStatement(PsiDoWhileStatement statement) {
    final Pair conditionPair = newPair();
    final Pair bodyPair = newPair();
    final Pair mergePair = newPair();

    breakJumps.put(statement, mergePair);
    continueJumps.put(statement, conditionPair);

    createJump(bodyPair);

    currentPair = bodyPair;
    statement.getBody().accept(this);
    createJump(conditionPair);

    currentPair = conditionPair;
    createConditionalJump(statement.getCondition(), bodyPair, mergePair);

    currentPair = mergePair;
  }

  @Override
  public void visitMethodCallExpression(PsiMethodCallExpression expression) {
    currentPair.block.add(Util.createPushStatement(
      factory, frameClassName, stackVarName, expression.getArgumentList().getExpressions(), PsiElement::getText));
    final Pair newPair = newPair();
    createJump(newPair);

    currentPair = newPair;

    final PsiElement parent = expression.getParent();
    if (parent instanceof PsiAssignmentExpression) {
      PsiAssignmentExpression assignment = (PsiAssignmentExpression)parent;
      currentPair.block.add(createStatement(assignment.getLExpression().getText() + " = " + retVarName + ";"));
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
    for (Pair theBlock : reachableBlocks) {
      if (theBlock.id != 0 && theBlock.block.getStatements().length == 0 && theBlock.jump instanceof Jump) {
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
        pair.terminate();
      }
    }

    return pairs;
  }
}
