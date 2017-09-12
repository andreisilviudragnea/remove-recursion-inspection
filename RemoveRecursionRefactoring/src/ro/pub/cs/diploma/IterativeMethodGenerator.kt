package ro.pub.cs.diploma

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import ro.pub.cs.diploma.ir.Block
import ro.pub.cs.diploma.ir.InlineVisitor
import ro.pub.cs.diploma.passes.*

class IterativeMethodGenerator(private val myFactory: PsiElementFactory,
                               private val myStyleManager: JavaCodeStyleManager,
                               private val myMethod: PsiMethod, private val myNameManager: NameManager) {

  private fun statement(text: String): PsiStatement = myFactory.statement(text)

  fun createIterativeBody(steps: Int) {
    replaceSingleStatementsWithBlockStatements(myMethod)
    if (steps == 1) {
      return
    }

    renameVariablesToUniqueNames(myMethod)
    if (steps == 2) {
      return
    }

    ReplaceForEachLoopsWithIteratorForLoops(myMethod).apply(myMethod)
    ReplaceForEachLoopsWithIndexedForLoops(myMethod).apply(myMethod)
    if (steps == 3) {
      return
    }

    extractRecursiveCallsToStatements(myMethod)
    if (steps == 4) {
      return
    }

    addFrameClass(myMethod, myNameManager)
    if (steps == 5) {
      return
    }

    val incorporatedBody = IncorporateBody(myNameManager, myFactory, myStyleManager).apply(myMethod) ?: return
    if (steps == 6) {
      return
    }

    ReplaceIdentifierWithFrameAccess(myNameManager, myFactory, incorporatedBody).apply(myMethod)
    if (steps == 7) {
      return
    }

    ReplaceDeclarationsHavingInitializersWithAssignments(myMethod, myNameManager, myFactory).apply(incorporatedBody)
    if (steps == 8) {
      return
    }

    val basicBlocksGenerator = BasicBlocksGenerator(myMethod, myNameManager, myFactory,
        incorporatedBody.extractStatementsContainingRecursiveCallsTo(myMethod))
    incorporatedBody.accept(basicBlocksGenerator)
    val blocks = basicBlocksGenerator.blocks

    val collect = blocks.map { it.toDot() }.flatten().toMutableList()
    collect.add(0, "node [shape=record];")

    val cfg = "digraph cfg {\n\t${collect.joinToString("\n\t")}\n}"

    val optimizedBlocks = applyBlocksOptimizations(steps, blocks)

    val pairs = optimizedBlocks
        .filter { block -> !block.isInlinable }
        .map { block ->
          val inlineVisitor = InlineVisitor(myFactory, myNameManager)
          block.accept(inlineVisitor)
          Pair.create(block.id, inlineVisitor.block)
        }.toList()

    val atLeastOneLabeledBreak = Ref(false)
    replaceReturnStatements(steps, pairs, atLeastOneLabeledBreak)

    val casesString = pairs.joinToString("") { pair -> "case ${pair.getFirst()}: ${pair.getSecond().text}" }

    incorporatedBody.replace(statement(
        (if (atLeastOneLabeledBreak.get()) "${myNameManager.switchLabelName}:" else "") +
            "switch (${myNameManager.frameVarName}.${myNameManager.blockFieldName})" +
            "{$casesString}"))
  }

  private fun applyBlocksOptimizations(steps: Int, blocks: List<Block>): List<Block> {
    if (steps == 9) {
      blocks.forEach { block -> block.setDoNotInline(true) }
      return blocks
    }

    val reachableBlocks = RemoveUnreachableBlocks().apply(blocks)

    if (steps == 10) {
      reachableBlocks.forEach { block -> block.setDoNotInline(true) }
      return reachableBlocks
    }

    val nonTrivialReachableBlocks = reachableBlocks
        .filter { it.inlineIfTrivial() }
        .toList()

    val collect = nonTrivialReachableBlocks.map { it.toDot() }.flatten().toMutableList()
    collect.add(0, "node [shape=record];")

    val cfg = "digraph cfg {\n\t${collect.joinToString("\n\t")}\n}"

    if (steps == 11) {
      nonTrivialReachableBlocks.forEach { block -> block.setDoNotInline(true) }
      return nonTrivialReachableBlocks
    }

    return nonTrivialReachableBlocks
  }

  private fun replaceReturnStatements(steps: Int,
                                      pairs: List<Pair<Int, PsiCodeBlock>>,
                                      atLeastOneLabeledBreak: Ref<Boolean>) {
    if (steps <= 12) {
      return
    }
    pairs.forEach { pair -> replaceReturnStatements(pair.getSecond(), myNameManager, atLeastOneLabeledBreak) }
  }

  private fun replaceReturnStatements(block: PsiCodeBlock,
                                      nameManager: NameManager,
                                      atLeastOneLabeledBreak: Ref<Boolean>) {
    for (statement in extractReturnStatements(block)) {
      val returnValue = statement.returnValue
      val parentBlock = PsiTreeUtil.getParentOfType(statement, PsiCodeBlock::class.java, true) ?: continue
      var anchor: PsiElement = statement
      if (returnValue != null) {
        anchor = parentBlock.addAfter(statement("${nameManager.retVarName} = ${returnValue.text};"), anchor)
      }
      anchor = parentBlock.addAfter(statement("${nameManager.stackVarName}.pop();"), anchor)
      val inLoop = PsiTreeUtil.getParentOfType(statement, PsiLoopStatement::class.java, true, PsiClass::class.java) != null
      atLeastOneLabeledBreak.set(atLeastOneLabeledBreak.get() || inLoop)
      parentBlock.addAfter(statement("break ${if (inLoop) nameManager.switchLabelName else ""};"), anchor)

      statement.delete()
    }
  }
}
