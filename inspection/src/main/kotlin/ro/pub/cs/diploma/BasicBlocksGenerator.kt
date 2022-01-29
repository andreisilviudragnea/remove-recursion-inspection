package ro.pub.cs.diploma

import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiBreakStatement
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiContinueStatement
import com.intellij.psi.PsiDoWhileStatement
import com.intellij.psi.PsiEmptyStatement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionListStatement
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiForStatement
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLoopStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiStatement
import com.intellij.psi.PsiSwitchLabelStatement
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.PsiWhileStatement
import com.intellij.psi.util.PsiTreeUtil
import com.siyeh.ig.psiutils.ExpressionUtils
import ro.pub.cs.diploma.ir.Block
import ro.pub.cs.diploma.ir.NormalStatement
import ro.pub.cs.diploma.ir.Statement
import ro.pub.cs.diploma.ir.UnconditionalJumpStatement

internal class BasicBlocksGenerator(
    private val myMethod: PsiMethod,
    private val myNameManager: NameManager,
    private val myStatementsContainingRecursiveCalls: Set<PsiStatement>
) : JavaRecursiveElementVisitor() {
    private val myFactory = myMethod.getFactory()
    private val myBlocks = ArrayList<Block>()
    private val myBreakTargets = HashMap<PsiStatement, Block>()
    private val myContinueTargets = HashMap<PsiStatement, Block>()

    private var myCounter: Int = 0
    private var myCurrentBlock: Block = newBlock()

    val blocks: List<Block>
        get() = myBlocks

    private fun newBlock(): Block {
        val block = Block(myCounter++)
        myBlocks.add(block)
        return block
    }

    private fun addStatement(text: String) {
        myCurrentBlock.add(myFactory.statement(text))
    }

    private fun processStatement(statement: PsiStatement) {
        if (myStatementsContainingRecursiveCalls.contains(statement) ||
            statement is PsiReturnStatement ||
            statement is PsiBreakStatement ||
            statement is PsiContinueStatement
        ) {
            statement.accept(this)
            return
        }

        val breakContinueReplacerVisitor = BreakContinueReplacerVisitor(myBreakTargets, myContinueTargets, myFactory, myCurrentBlock)
        statement.accept(breakContinueReplacerVisitor)
        myCurrentBlock.add(statement)
    }

    override fun visitReturnStatement(statement: PsiReturnStatement) {
        myCurrentBlock.addReturnStatement(statement)
    }

    private fun processStatement(
        statement: PsiStatement,
        targetStatement: PsiStatement,
        targets: Map<PsiStatement, Block>
    ) {
        val block = targets[targetStatement]
        if (block == null) {
            myCurrentBlock.add(statement)
            return
        }
        myCurrentBlock.addUnconditionalJump(block)
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
        if (PsiPrimitiveType.VOID == myMethod.returnType && statements.last() !is PsiReturnStatement) {
            myCurrentBlock.addReturnStatement(myFactory.statement("return;") as PsiReturnStatement)
        }
    }

    override fun visitBlockStatement(blockStatement: PsiBlockStatement) {
        blockStatement.codeBlock.statements.forEach { this.processStatement(it) }
    }

    override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
        myCurrentBlock.add(
            myFactory.createPushStatement(
                myNameManager.frameClassName, myNameManager.stackVarName,
                expression.argumentList.expressions
            ) { it.text }
        )

        val block = newBlock()
        block.setDoNotInline(true)
        myCurrentBlock.addUnconditionalJump(block)

        myCurrentBlock = block

        val returnType = myMethod.returnType ?: return
        if (returnType != PsiPrimitiveType.VOID) {
            val parent = PsiTreeUtil.getParentOfType(expression, PsiStatement::class.java, true) ?: return
            expression.replace(myFactory.expression(myNameManager.retVarName))
            myCurrentBlock.add(parent)
        }
    }

    override fun visitIfStatement(statement: PsiIfStatement) {
        val thenBlock = newBlock()
        val elseBranch = statement.elseBranch
        val elseBlock = if (elseBranch != null) newBlock() else null
        val mergeBlock = newBlock()

        myCurrentBlock.addConditionalJump(statement.condition ?: return, thenBlock, elseBlock ?: mergeBlock)

        myCurrentBlock = thenBlock
        val thenBranch = statement.thenBranch ?: return
        thenBranch.accept(this)
        myCurrentBlock.addUnconditionalJump(mergeBlock)

        if (elseBranch != null && elseBlock != null) {
            myCurrentBlock = elseBlock
            elseBranch.accept(this)
            myCurrentBlock.addUnconditionalJump(mergeBlock)
        }

        myCurrentBlock = mergeBlock
    }

    private fun addStatements(statement: PsiStatement) {
        when (statement) {
            is PsiExpressionStatement -> addStatement("${statement.expression.text};")
            is PsiExpressionListStatement -> statement.expressionList.expressions.forEach { addStatement("${it.text};") }
        }
    }

    private fun visitLoop(
        condition: PsiExpression?,
        update: PsiStatement?,
        statement: PsiLoopStatement,
        atLeastOnce: Boolean
    ) {
        val expression = ExpressionUtils.computeConstantExpression(condition)
        val theCondition = if (expression is Boolean && expression == java.lang.Boolean.TRUE) null else condition

        val conditionBlock = if (theCondition != null) newBlock() else null
        val bodyBlock = newBlock()
        val updateBlock = if (update != null) newBlock() else null
        val mergeBlock = newBlock()

        val actualConditionBlock = conditionBlock ?: bodyBlock
        val actualUpdateBlock = updateBlock ?: actualConditionBlock

        myBreakTargets[statement] = mergeBlock
        myContinueTargets[statement] = actualUpdateBlock

        myCurrentBlock.addUnconditionalJump(if (atLeastOnce) bodyBlock else actualConditionBlock)

        if (theCondition != null && conditionBlock != null) {
            myCurrentBlock = conditionBlock
            myCurrentBlock.addConditionalJump(theCondition, bodyBlock, mergeBlock)
        }

        if (update != null && updateBlock != null) {
            myCurrentBlock = updateBlock
            addStatements(update)
            myCurrentBlock.addUnconditionalJump(actualConditionBlock)
        }

        myCurrentBlock = bodyBlock
        val body = statement.body ?: return
        body.accept(this)
        myCurrentBlock.addUnconditionalJump(actualUpdateBlock)

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

    override fun visitSwitchStatement(switchStatement: PsiSwitchStatement) {
        val mergeBlock = newBlock()
        myBreakTargets[switchStatement] = mergeBlock

        val body = switchStatement.body ?: return
        val statements = ArrayList<Statement>()
        val oldCurrentBlock = myCurrentBlock
        var previousCurrentBlock: Block? = null
        for (statement in body.statements) {
            when (statement) {
                is PsiSwitchLabelStatement -> statements.add(NormalStatement(statement))
                is PsiBlockStatement -> {
                    val newBlock = newBlock()
                    statements.add(UnconditionalJumpStatement(Ref.create(newBlock)))

                    if (previousCurrentBlock != null && !previousCurrentBlock.isFinished) {
                        previousCurrentBlock.addUnconditionalJump(newBlock)
                    }

                    myCurrentBlock = newBlock
                    statement.accept(this)
                    previousCurrentBlock = myCurrentBlock
                }
            }
        }

        if (previousCurrentBlock != null && !previousCurrentBlock.isFinished) {
            previousCurrentBlock.addUnconditionalJump(mergeBlock)
        }

        myCurrentBlock = oldCurrentBlock
        val expression = switchStatement.expression ?: return
        myCurrentBlock.addSwitchStatement(expression, statements)

        myCurrentBlock = mergeBlock
    }

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {}

    override fun visitClass(aClass: PsiClass) {}
}
