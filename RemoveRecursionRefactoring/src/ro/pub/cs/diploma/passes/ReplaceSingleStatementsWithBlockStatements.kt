package ro.pub.cs.diploma.passes

import com.intellij.psi.*
import ro.pub.cs.diploma.getFactory
import ro.pub.cs.diploma.statement
import java.util.*

class ReplaceSingleStatementsWithBlockStatements(private val myFactory: PsiElementFactory) : Pass<PsiMethod, List<PsiStatement>, Any> {

  override fun collect(method: PsiMethod): List<PsiStatement> {
    val statements = ArrayList<PsiStatement>()
    method.accept(object : JavaRecursiveElementVisitor() {
      override fun visitIfStatement(statement: PsiIfStatement) {
        super.visitIfStatement(statement)
        statements.add(statement.thenBranch ?: return)
        val elseBranch = statement.elseBranch
        if (elseBranch != null) {
          statements.add(elseBranch)
        }
      }

      override fun visitForStatement(statement: PsiForStatement) {
        super.visitForStatement(statement)
        statements.add(statement.body ?: return)
      }

      override fun visitWhileStatement(statement: PsiWhileStatement) {
        super.visitWhileStatement(statement)
        statements.add(statement.body ?: return)
      }

      override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
        super.visitDoWhileStatement(statement)
        statements.add(statement.body ?: return)
      }

      override fun visitForeachStatement(statement: PsiForeachStatement) {
        super.visitForeachStatement(statement)
        statements.add(statement.body ?: return)
      }

      override fun visitSwitchStatement(statement: PsiSwitchStatement) {
        super.visitSwitchStatement(statement)
        val block = statement.body ?: return
        val factory = statement.getFactory()
        var currentCodeBlock: PsiCodeBlock? = null
        var currentLabelStatement: PsiSwitchLabelStatement? = null
        for (psiStatement in block.statements) {
          if (psiStatement is PsiSwitchLabelStatement) {
            addBlock(block, factory, currentLabelStatement, currentCodeBlock)
            currentLabelStatement = psiStatement
            currentCodeBlock = factory.createCodeBlock()
            continue
          }
          if (currentCodeBlock != null && psiStatement !is PsiEmptyStatement) {
            currentCodeBlock.add(psiStatement)
          }
          psiStatement.delete()
        }
        addBlock(block, factory, currentLabelStatement, currentCodeBlock)
      }

      private fun addBlock(block: PsiCodeBlock,
                           factory: PsiElementFactory,
                           currentLabelStatement: PsiSwitchLabelStatement?,
                           currentCodeBlock: PsiCodeBlock?) {
        if (currentLabelStatement == null || currentCodeBlock == null || currentCodeBlock.statements.isEmpty()) {
          return
        }
        val currentCodeBlockStatements = currentCodeBlock.statements
        val statementToAdd: PsiStatement
        statementToAdd = if (currentCodeBlockStatements.size == 1 && currentCodeBlockStatements[0] is PsiBlockStatement) {
          currentCodeBlockStatements[0]
        }
        else {
          factory.statement(currentCodeBlock.text)
        }
        block.addAfter(statementToAdd, currentLabelStatement)
      }
    })
    return statements
  }

  override fun transform(statements: List<PsiStatement>): Any? {
    statements.forEach { statement ->
      if (statement is PsiEmptyStatement) {
        statement.replace(myFactory.createExpressionFromText("{}", null))
      }
      else if (statement !is PsiBlockStatement) {
        statement.replace(myFactory.statement("{${statement.text}}"))
      }
    }
    return null
  }
}
