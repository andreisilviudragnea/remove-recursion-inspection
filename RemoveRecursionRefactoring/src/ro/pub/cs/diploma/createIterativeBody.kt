package ro.pub.cs.diploma

import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiMethod
import ro.pub.cs.diploma.ir.Block
import ro.pub.cs.diploma.ir.InlineVisitor
import ro.pub.cs.diploma.passes.*
import kotlin.coroutines.experimental.buildSequence

fun createIterativeBody(steps: Int, method: PsiMethod, nameManager: NameManager) = buildSequence {
  replaceSingleStatementsWithBlockStatements(method)
  yield(Unit)

  renameVariablesToUniqueNames(method)
  yield(Unit)

  replaceForEachLoopsWithIteratorForLoops(method)
  replaceForEachLoopsWithIndexedForLoops(method)
  yield(Unit)

  extractRecursiveCallsToStatements(method)
  yield(Unit)

  addFrameClass(method, nameManager)
  yield(Unit)

  val incorporatedBody = incorporateBody(method, nameManager) ?: return@buildSequence
  yield(Unit)

  replaceReferencesWithFieldAccesses(method, incorporatedBody, nameManager)
  yield(Unit)

  replaceDeclarationsHavingInitializersWithAssignments(method, incorporatedBody, nameManager)
  yield(Unit)

  val basicBlocksGenerator = BasicBlocksGenerator(method, nameManager, incorporatedBody.extractStatementsContainingRecursiveCallsTo(method))
  incorporatedBody.accept(basicBlocksGenerator)
  val blocks = basicBlocksGenerator.blocks

  val collect = blocks.map { it.toDot() }.flatten().toMutableList()
  collect.add(0, "node [shape=record];")

  val cfg = "digraph cfg {\n\t${collect.joinToString("\n\t")}\n}"

  val optimizedBlocks = applyBlocksOptimizations(steps, blocks)

  val factory = method.getFactory()

  val pairs = optimizedBlocks
      .filter { block -> !block.isInlinable }
      .map { block ->
        val inlineVisitor = InlineVisitor(factory, nameManager)
        block.accept(inlineVisitor)
        Pair.create(block.id, inlineVisitor.block)
      }.toList()

  val atLeastOneLabeledBreak = Ref(false)
  if (steps == 13) {
    pairs.forEach { pair -> replaceReturnStatements(pair.getSecond(), nameManager, atLeastOneLabeledBreak) }
  }

  val casesString = pairs.joinToString("") { pair -> "case ${pair.getFirst()}: ${pair.getSecond().text}" }

  incorporatedBody.replace(factory.statement(
      (if (atLeastOneLabeledBreak.get()) "${nameManager.switchLabelName}:" else "") +
          "switch (${nameManager.frameVarName}.${nameManager.blockFieldName})" +
          "{$casesString}"))
}

private fun applyBlocksOptimizations(steps: Int, blocks: List<Block>): List<Block> {
  if (steps == 9) {
    blocks.forEach { block -> block.setDoNotInline(true) }
    return blocks
  }

  val reachableBlocks = removeUnreachableBlocks(blocks)

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
