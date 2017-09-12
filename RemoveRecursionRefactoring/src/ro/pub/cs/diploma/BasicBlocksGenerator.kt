package ro.pub.cs.diploma

import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.psiutils.ExpressionUtils
import ro.pub.cs.diploma.ir.Block
import ro.pub.cs.diploma.ir.NormalStatement
import ro.pub.cs.diploma.ir.Statement
import ro.pub.cs.diploma.ir.UnconditionalJumpStatement

import java.util.*

internal class BasicBlocksGenerator(private val myMethod: PsiMethod,
                                    private val myNameManager: NameManager,
                                    private val myFactory: PsiElementFactory,
                                    private val myStatementsContainingRecursiveCalls: Set<PsiStatement>) : JavaRecursiveElementVisitor() {

  private val myBlocks = ArrayList<Block>()
  private val myBreakTargets = HashMap<PsiStatement, Block>()
  private val myContinueTargets = HashMap<PsiStatement, Block>()

  private var myCurrentBlock: Block
  private var myCounter: Int = 0

  val blocks: List<Block>
    get() = myBlocks

  init {
    myCurrentBlock = newBlock()
  }

  private fun newBlock(): Block {
    val block = Block(myCounter++)
    myBlocks.add(block)
    return block
  }

  private fun addStatement(statement: PsiStatement?) {
    myCurrentBlock.add(statement!!)
  }

  private fun addStatement(text: String) {
    myCurrentBlock.add(myFactory.statement(text))
  }

  private fun addUnconditionalJumpStatement(block: Block) {
    myCurrentBlock.addUnconditionalJump(Ref.create(block))
  }

  private fun addConditionalJumpStatement(condition: PsiExpression,
                                          thenBlock: Block,
                                          jumpBlock: Block) {
    myCurrentBlock.addConditionalJump(condition, Ref.create(thenBlock), Ref.create(jumpBlock))
  }

  private fun addReturnStatement(statement: PsiReturnStatement) {
    myCurrentBlock.addReturnStatement(statement)
  }

  private fun processStatement(statement: PsiStatement) {
    if (myStatementsContainingRecursiveCalls.contains(statement) ||
        statement is PsiReturnStatement ||
        statement is PsiBreakStatement ||
        statement is PsiContinueStatement) {
      statement.accept(this)
      return
    }

    val breakContinueReplacerVisitor = BreakContinueReplacerVisitor(myBreakTargets, myContinueTargets, myFactory, myCurrentBlock)
    statement.accept(breakContinueReplacerVisitor)
    addStatement(statement)
  }

  override fun visitReturnStatement(statement: PsiReturnStatement) {
    addReturnStatement(statement)
  }

  private fun processStatement(statement: PsiStatement,
                               targetStatement: PsiStatement,
                               targets: Map<PsiStatement, Block>) {
    val block = targets[targetStatement]
    if (block == null) {
      addStatement(statement)
      return
    }
    addUnconditionalJumpStatement(block)
  }

  override fun visitBreakStatement(statement: PsiBreakStatement) {
    val exitedStatement = statement.findExitedStatement() ?: return
    processStatement(statement, exitedStatement, myBreakTargets)
  }

  override fun visitContinueStatement(statement: PsiContinueStatement) {
    val continuedStatement = statement.findContinuedStatement() ?: return
    processStatement(statement, continuedStatement, myContinueTargets)
  }

  override fun visitCodeBlock(block: PsiCodeBlock) {
    block.statements.forEach { this.processStatement(it) }

    val statements = block.statements
    // This is a hack, this method gets called only for the method block, not for blocks of block statements.
    if (PsiPrimitiveType.VOID == myMethod.returnType && statements[statements.size - 1] !is PsiReturnStatement) {
      addReturnStatement(myFactory.statement("return;") as PsiReturnStatement)
    }
  }

  override fun visitBlockStatement(blockStatement: PsiBlockStatement) {
    blockStatement.codeBlock.statements.forEach { this.processStatement(it) }
  }

  override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
    addStatement(Utilss.createPushStatement(myFactory, myNameManager.frameClassName, myNameManager.stackVarName,
        expression.argumentList.expressions) { obj: PsiElement -> obj.text })

    val block = newBlock()
    block.setDoNotInline(true)
    addUnconditionalJumpStatement(block)

    myCurrentBlock = block

    val returnType = myMethod.returnType ?: return
    if (returnType != PsiPrimitiveType.VOID) {
      val parent = PsiTreeUtil.getParentOfType(expression, PsiStatement::class.java, true)
      expression.replace(myFactory.createExpressionFromText(myNameManager.retVarName, null))
      addStatement(parent)
    }
  }

  override fun visitIfStatement(statement: PsiIfStatement) {
    val thenBlock = newBlock()
    val elseBranch = statement.elseBranch
    val elseBlock = if (elseBranch != null) newBlock() else null
    val mergeBlock = newBlock()

    val condition = statement.condition ?: return
    addConditionalJumpStatement(condition, thenBlock, elseBlock ?: mergeBlock)

    myCurrentBlock = thenBlock
    val thenBranch = statement.thenBranch ?: return
    thenBranch.accept(this)
    addUnconditionalJumpStatement(mergeBlock)

    if (elseBranch != null && elseBlock != null) {
      myCurrentBlock = elseBlock
      elseBranch.accept(this)
      addUnconditionalJumpStatement(mergeBlock)
    }

    myCurrentBlock = mergeBlock
  }

  private fun addStatements(statement: PsiStatement) {
    if (statement is PsiExpressionStatement) {
      addStatement("${statement.expression.text};")
      return
    }
    if (statement is PsiExpressionListStatement) {
      for (expression in statement.expressionList.expressions) {
        addStatement("${expression.text};")
      }
    }
  }

  private fun visitLoop(condition: PsiExpression?,
                        update: PsiStatement?,
                        statement: PsiLoopStatement,
                        atLeastOnce: Boolean) {
    val expression = ExpressionUtils.computeConstantExpression(condition)
    val theCondition: PsiExpression?
    theCondition = if (expression is Boolean && expression == java.lang.Boolean.TRUE) null else condition

    val conditionBlock = if (theCondition != null) newBlock() else null
    val bodyBlock = newBlock()
    val updateBlock = if (update != null) newBlock() else null
    val mergeBlock = newBlock()

    val actualConditionBlock = conditionBlock ?: bodyBlock
    val actualUpdateBlock = updateBlock ?: actualConditionBlock

    myBreakTargets.put(statement, mergeBlock)
    myContinueTargets.put(statement, actualUpdateBlock)

    addUnconditionalJumpStatement(if (atLeastOnce) bodyBlock else actualConditionBlock)

    if (conditionBlock != null) {
      myCurrentBlock = conditionBlock
      addConditionalJumpStatement(theCondition!!, bodyBlock, mergeBlock)
    }

    if (update != null && updateBlock != null) {
      myCurrentBlock = updateBlock
      addStatements(update)
      addUnconditionalJumpStatement(actualConditionBlock)
    }

    myCurrentBlock = bodyBlock
    val body = statement.body ?: return
    body.accept(this)
    addUnconditionalJumpStatement(actualUpdateBlock)

    myCurrentBlock = mergeBlock
  }

  override fun visitWhileStatement(statement: PsiWhileStatement) {
    val condition = statement.condition ?: return
    visitLoop(condition, null, statement, false)
  }

  override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
    val condition = statement.condition ?: return
    visitLoop(condition, null, statement, true)
  }

  override fun visitForStatement(statement: PsiForStatement) {
    val initialization = statement.initialization
    if (initialization != null && initialization !is PsiEmptyStatement) {
      addStatements(initialization)
    }

    visitLoop(statement.condition, statement.update, statement, false)
  }

  override fun visitSwitchStatement(statement: PsiSwitchStatement) {
    val mergeBlock = newBlock()
    myBreakTargets.put(statement, mergeBlock)

    val body = statement.body ?: return
    val statements = ArrayList<Statement>()
    val oldCurrentBlock = myCurrentBlock
    var previousCurrentBlock: Block? = null
    for (psiStatement in body.statements) {
      if (psiStatement is PsiSwitchLabelStatement) {
        statements.add(NormalStatement(psiStatement))
        continue
      }
      if (psiStatement is PsiBlockStatement) {
        val newBlock = newBlock()
        statements.add(UnconditionalJumpStatement(Ref.create(newBlock)))

        if (previousCurrentBlock != null && !previousCurrentBlock.isFinished) {
          previousCurrentBlock.addUnconditionalJump(Ref.create(newBlock))
        }

        myCurrentBlock = newBlock
        psiStatement.accept(this)
        previousCurrentBlock = myCurrentBlock
      }
    }

    if (previousCurrentBlock != null && !previousCurrentBlock.isFinished) {
      previousCurrentBlock.addUnconditionalJump(Ref.create(mergeBlock))
    }

    myCurrentBlock = oldCurrentBlock
    val expression = statement.expression ?: return
    myCurrentBlock.addSwitchStatement(expression, statements)

    myCurrentBlock = mergeBlock
  }

  override fun visitLambdaExpression(expression: PsiLambdaExpression) {}

  override fun visitClass(aClass: PsiClass) {}
}
