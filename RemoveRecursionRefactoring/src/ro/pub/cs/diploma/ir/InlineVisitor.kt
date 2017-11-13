package ro.pub.cs.diploma.ir

import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElementFactory
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.statement

class InlineVisitor(private val factory: PsiElementFactory, nameManager: NameManager) : Visitor {
  private val blockSet: String = "${nameManager.frameVarName}.${nameManager.blockFieldName} = "
  val block: PsiCodeBlock = factory.createCodeBlock()
  private var currentBlock: PsiCodeBlock = block

  private fun addStatement(text: String) {
    currentBlock.add(factory.statement(text))
  }

  private fun addStatement(statement: WrapperStatement) {
    currentBlock.add(statement.statement)
  }

  private fun addBreak() {
    addStatement("break;")
  }

  private fun addBlockSet(value: String) {
    addStatement("$blockSet$value;")
    addBreak()
  }

  override fun visit(block: Block) {
    block.statements.forEach { statement -> statement.accept(this) }
  }

  private fun inline(block: Block): PsiCodeBlock? {
    var psiBlock: PsiCodeBlock? = null
    if (block.isInlinable) {
      psiBlock = factory.createCodeBlock()
      val oldCurrentBlock = currentBlock
      currentBlock = psiBlock
      block.accept(this)
      currentBlock = oldCurrentBlock
    }
    return psiBlock
  }

  private fun getConcreteBlock(block: Block, psiBlock: PsiCodeBlock?): PsiCodeBlock {
    val concretePsiBlock: PsiCodeBlock
    if (psiBlock != null) {
      concretePsiBlock = psiBlock
    } else {
      concretePsiBlock = factory.createCodeBlock()
      val oldCurrentBlock = currentBlock
      currentBlock = concretePsiBlock
      addBlockSet(block.id.toString())
      currentBlock = oldCurrentBlock
    }
    return concretePsiBlock
  }

  override fun visit(conditionalJumpStatement: ConditionalJumpStatement) {
    val thenBlock = conditionalJumpStatement.thenBlock
    val thenPsiBlock = inline(thenBlock)

    val elseBlock = conditionalJumpStatement.elseBlock
    val elsePsiBlock = inline(elseBlock)

    val conditionText = conditionalJumpStatement.condition.text

    if (thenPsiBlock == null && elsePsiBlock == null) {
      addBlockSet("$conditionText ? ${thenBlock.id} : ${elseBlock.id}")
      return
    }

    val concreteThenPsiBlock = getConcreteBlock(thenBlock, thenPsiBlock)
    val concreteElsePsiBlock = getConcreteBlock(elseBlock, elsePsiBlock)

    addStatement("if ($conditionText) ${concreteThenPsiBlock.text} else ${concreteElsePsiBlock.text}")
  }

  override fun visit(normalStatement: NormalStatement) {
    addStatement(normalStatement)
  }

  override fun visit(returnStatement: ReturnStatement) {
    addStatement(returnStatement)
  }

  override fun visit(unconditionalJumpStatement: UnconditionalJumpStatement) {
    val block = unconditionalJumpStatement.block
    val psiBlock = inline(block)

    if (psiBlock != null) {
      psiBlock.statements.forEach { currentBlock.add(it) }
      return
    }

    addBlockSet(block.id.toString())
  }

  override fun visit(switchStatement: SwitchStatement) {
    val statements = switchStatement.statements
    val psiBlock = factory.createCodeBlock()
    val oldCurrentBlock = currentBlock
    currentBlock = psiBlock
    for (statement in statements) {
      statement.accept(this)
    }
    currentBlock = oldCurrentBlock

    addStatement("switch (${switchStatement.expression.text}) ${psiBlock.text}")
    addBreak()
  }
}
