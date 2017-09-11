package ro.pub.cs.diploma;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import ro.pub.cs.diploma.ir.Block;
import ro.pub.cs.diploma.ir.InlineVisitor;
import ro.pub.cs.diploma.passes.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class IterativeMethodGenerator {
  @NotNull private final PsiElementFactory myFactory;
  @NotNull private final JavaCodeStyleManager myStyleManager;
  @NotNull private final PsiMethod myMethod;
  @NotNull private final NameManager myNameManager;

  private IterativeMethodGenerator(@NotNull PsiElementFactory factory,
                                   @NotNull JavaCodeStyleManager styleManager,
                                   @NotNull PsiMethod method, @NotNull NameManager nameManager) {
    myFactory = factory;
    myStyleManager = styleManager;
    myMethod = method;
    myNameManager = nameManager;
  }

  @NotNull
  public static IterativeMethodGenerator getInstance(@NotNull final PsiElementFactory factory,
                                                     @NotNull final JavaCodeStyleManager styleManager,
                                                     @NotNull final PsiMethod method,
                                                     @NotNull final NameManager nameManager) {
    return new IterativeMethodGenerator(factory, styleManager, method, nameManager);
  }

  @NotNull
  private PsiStatement statement(@NotNull String text) {
    return myFactory.createStatementFromText(text, null);
  }

  public void createIterativeBody(int steps) {
    ReplaceSingleStatementsWithBlockStatements.getInstance(myFactory).apply(myMethod);
    if (steps == 1) {
      return;
    }

    RenameVariablesToUniqueNames.getInstance(myMethod).apply(myMethod);
    if (steps == 2) {
      return;
    }

    ReplaceForEachLoopsWithIteratorForLoops.getInstance(myMethod).apply(myMethod);
    ReplaceForEachLoopsWithIndexedForLoops.getInstance(myMethod).apply(myMethod);
    if (steps == 3) {
      return;
    }

    ExtractRecursiveCallsToStatements.getInstance(myMethod).apply(myMethod);
    if (steps == 4) {
      return;
    }

    AddFrameClass.getInstance(myMethod, myNameManager).apply(myMethod);
    if (steps == 5) {
      return;
    }

    final PsiCodeBlock incorporatedBody = IncorporateBody.getInstance(myNameManager, myFactory, myStyleManager).apply(myMethod);
    if (incorporatedBody == null) {
      return;
    }
    if (steps == 6) {
      return;
    }

    ReplaceIdentifierWithFrameAccess.getInstance(myNameManager, myFactory, incorporatedBody).apply(myMethod);
    if (steps == 7) {
      return;
    }

    ReplaceDeclarationsHavingInitializersWithAssignments.getInstance(myMethod, myNameManager, myFactory).apply(incorporatedBody);
    if (steps == 8) {
      return;
    }

    final BasicBlocksGenerator basicBlocksGenerator =
      new BasicBlocksGenerator(myMethod, myNameManager, myFactory,
                               RecursionUtil.extractStatementsContainingRecursiveCalls(incorporatedBody, myMethod));
    incorporatedBody.accept(basicBlocksGenerator);
    final List<Block> blocks = basicBlocksGenerator.getBlocks();

    final List<String> collect = blocks.stream().map(Block::toDot).flatMap(Collection::stream).collect(Collectors.toList());
    collect.add(0, "node [shape=record];");

    final String cfg = String.format("digraph cfg {\n\t%s\n}", collect.stream().collect(Collectors.joining("\n\t")));

    final List<Block> optimizedBlocks = applyBlocksOptimizations(steps, blocks);

    final List<Pair<Integer, PsiCodeBlock>> pairs = optimizedBlocks
      .stream()
      .filter(block -> !block.isInlinable())
      .map(block -> {
        InlineVisitor inlineVisitor = new InlineVisitor(myFactory, myNameManager);
        block.accept(inlineVisitor);
        return Pair.create(block.getId(), inlineVisitor.getBlock());
      }).collect(Collectors.toList());

    final Ref<Boolean> atLeastOneLabeledBreak = new Ref<>(false);
    replaceReturnStatements(steps, pairs, atLeastOneLabeledBreak);

    final String casesString = pairs.stream().map(pair -> "case " + pair.getFirst() + ":" + pair.getSecond().getText())
      .collect(Collectors.joining(""));

    incorporatedBody.replace(statement(
      (atLeastOneLabeledBreak.get() ? myNameManager.getSwitchLabelName() + ":" : "") +
      "switch(" + myNameManager.getFrameVarName() + "." + myNameManager.getBlockFieldName() + "){" + casesString + "}"));
  }

  private List<Block> applyBlocksOptimizations(final int steps, @NotNull final List<Block> blocks) {
    if (steps == 9) {
      blocks.forEach(block -> block.setDoNotInline(true));
      return blocks;
    }

    final List<Block> reachableBlocks = RemoveUnreachableBlocks.getInstance().apply(blocks);

    if (steps == 10) {
      reachableBlocks.forEach(block -> block.setDoNotInline(true));
      return reachableBlocks;
    }

    final List<Block> nonTrivialReachableBlocks = reachableBlocks
      .stream()
      .filter(Block::inlineIfTrivial)
      .collect(Collectors.toList());

    final List<String> collect = nonTrivialReachableBlocks.stream().map(Block::toDot).flatMap(Collection::stream).collect(Collectors.toList());
    collect.add(0, "node [shape=record];");

    final String cfg = String.format("digraph cfg {\n\t%s\n}", collect.stream().collect(Collectors.joining("\n\t")));

    if (steps == 11) {
      nonTrivialReachableBlocks.forEach(block -> block.setDoNotInline(true));
      return nonTrivialReachableBlocks;
    }

    return nonTrivialReachableBlocks;
  }

  private void replaceReturnStatements(final int steps,
                                       @NotNull final List<Pair<Integer, PsiCodeBlock>> pairs,
                                       @NotNull final Ref<Boolean> atLeastOneLabeledBreak) {
    if (steps <= 12) {
      return;
    }
    pairs.forEach(pair -> replaceReturnStatements(pair.getSecond(), myNameManager, atLeastOneLabeledBreak));
  }

  private void replaceReturnStatements(@NotNull final PsiCodeBlock block,
                                       @NotNull final NameManager nameManager,
                                       @NotNull final Ref<Boolean> atLeastOneLabeledBreak) {
    for (final PsiReturnStatement statement : Visitors.extractReturnStatements(block)) {
      final PsiExpression returnValue = statement.getReturnValue();
      final boolean hasExpression = returnValue != null;
      final PsiCodeBlock parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock.class, true);
      if (parentBlock == null) {
        continue;
      }
      PsiElement anchor = statement;
      if (hasExpression) {
        anchor = parentBlock.addAfter(statement(nameManager.getRetVarName() + " = " + returnValue.getText() + ";"), anchor);
      }
      anchor = parentBlock.addAfter(statement(nameManager.getStackVarName() + ".pop();"), anchor);
      final boolean inLoop = PsiTreeUtil.getParentOfType(statement, PsiLoopStatement.class, true, PsiClass.class) != null;
      atLeastOneLabeledBreak.set(atLeastOneLabeledBreak.get() || inLoop);
      parentBlock.addAfter(statement("break " + (inLoop ? nameManager.getSwitchLabelName() : "") + ";"), anchor);

      statement.delete();
    }
  }
}
