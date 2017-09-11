package ro.pub.cs.diploma.ir

import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiStatement
import ro.pub.cs.diploma.NameManager

class InlineVisitor(private val factory: PsiElementFactory, nameManager: NameManager) : Visitor {
  private val blockSet: String = "${nameManager.frameVarName}.${nameManager.blockFieldName} = "
  val block: PsiCodeBlock

  private var currentBlock: PsiCodeBlock

  private fun newBlock(): PsiCodeBlock = factory.createCodeBlock()

  private fun addStatement(text: String) {
    currentBlock.add(factory.createStatementFromText(text, null))
  }

  private fun addStatement(statement: WrapperStatement) {
    currentBlock.add(statement.statement)
  }

  private fun addStatement(statement: PsiStatement) {
    currentBlock.add(statement)
  }

  private fun addBreak() {
    addStatement("break;")
  }

  private fun addBlockSet(`val`: String) {
    addStatement("$blockSet$`val`;")
    addBreak()
  }

  init {
    block = newBlock()

    currentBlock = block
  }

  override fun visit(block: Block) {
    block.statements.forEach { statement -> statement.accept(this) }
  }

  private fun inline(block: Block): PsiCodeBlock? {
    var psiBlock: PsiCodeBlock? = null
    if (block.isInlinable) {
      psiBlock = newBlock()
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
    }
    else {
      concretePsiBlock = newBlock()
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
      psiBlock.statements.forEach { this.addStatement(it) }
      return
    }

    addBlockSet(block.id.toString())
  }

  override fun visit(switchStatement: SwitchStatement) {
    val statements = switchStatement.statements
    val psiBlock = newBlock()
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
