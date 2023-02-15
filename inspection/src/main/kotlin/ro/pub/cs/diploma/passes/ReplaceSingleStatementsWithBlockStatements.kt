package ro.pub.cs.diploma.passes

import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiBlockStatement
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiDoWhileStatement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiEmptyStatement
import com.intellij.psi.PsiForStatement
import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiStatement
import com.intellij.psi.PsiSwitchLabelStatement
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.PsiWhileStatement
import ro.pub.cs.diploma.expression
import ro.pub.cs.diploma.getFactory
import ro.pub.cs.diploma.statement

fun replaceSingleStatementsWithBlockStatements(method: PsiMethod) {
    val factory = method.getFactory()
    method.accept(object : JavaRecursiveElementVisitor() {
        private fun process(statement: PsiStatement) {
            when (statement) {
                is PsiEmptyStatement -> {
                    statement.replace(factory.expression("{}"))
                }
                !is PsiBlockStatement -> {
                    statement.replace(factory.statement("{${statement.text}}"))
                }
            }
        }

        override fun visitIfStatement(statement: PsiIfStatement) {
            super.visitIfStatement(statement)
            process(statement.thenBranch ?: return)
            process(statement.elseBranch ?: return)
        }

        override fun visitForStatement(statement: PsiForStatement) {
            super.visitForStatement(statement)
            process(statement.body ?: return)
        }

        override fun visitWhileStatement(statement: PsiWhileStatement) {
            super.visitWhileStatement(statement)
            process(statement.body ?: return)
        }

        override fun visitDoWhileStatement(statement: PsiDoWhileStatement) {
            super.visitDoWhileStatement(statement)
            process(statement.body ?: return)
        }

        override fun visitForeachStatement(statement: PsiForeachStatement) {
            super.visitForeachStatement(statement)
            process(statement.body ?: return)
        }

        override fun visitSwitchStatement(statement: PsiSwitchStatement) {
            super.visitSwitchStatement(statement)
            val block = statement.body ?: return
            var currentLabelStatement: PsiSwitchLabelStatement? = null
            var currentCodeBlock: PsiCodeBlock? = null
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

        private fun addBlock(
            block: PsiCodeBlock,
            factory: PsiElementFactory,
            currentLabelStatement: PsiSwitchLabelStatement?,
            currentCodeBlock: PsiCodeBlock?
        ) {
            if (currentLabelStatement == null || currentCodeBlock == null || currentCodeBlock.statements.isEmpty()) {
                return
            }
            val currentCodeBlockStatements = currentCodeBlock.statements
            val statementToAdd = if (currentCodeBlockStatements.size == 1 && currentCodeBlockStatements[0] is PsiBlockStatement) {
                currentCodeBlockStatements[0]
            } else {
                factory.statement(currentCodeBlock.text)
            }
            block.addAfter(statementToAdd, currentLabelStatement)
        }
    })
}
