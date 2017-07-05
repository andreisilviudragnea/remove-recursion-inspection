package ro.pub.cs.diploma;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.psiutils.BlockUtils;

import java.util.Collection;

class Refactorings {
    /**
     * @see com.siyeh.ipp.forloop.ReplaceForLoopWithWhileLoopIntention#processIntention(PsiElement)
     */
    static void replaceForStatementWithWhileStatement(PsiForStatement forStatement) {
        PsiStatement initialization = forStatement.getInitialization();
        final PsiElementFactory factory = JavaPsiFacade.getElementFactory(forStatement.getProject());
        final PsiWhileStatement whileStatement = (PsiWhileStatement)
                factory.createStatementFromText("while(true) {}", forStatement);
        final PsiExpression forCondition = forStatement.getCondition();
        final PsiExpression whileCondition = whileStatement.getCondition();
        if (forCondition != null) {
            assert whileCondition != null;
            whileCondition.replace(forCondition);
        }
        final PsiBlockStatement blockStatement = (PsiBlockStatement) whileStatement.getBody();
        if (blockStatement == null) {
            return;
        }
        final PsiStatement forStatementBody = forStatement.getBody();
        final PsiElement loopBody;
        if (forStatementBody instanceof PsiBlockStatement) {
            final PsiBlockStatement newWhileBody = (PsiBlockStatement) blockStatement.replace(forStatementBody);
            loopBody = newWhileBody.getCodeBlock();
        } else {
            final PsiCodeBlock codeBlock = blockStatement.getCodeBlock();
            if (forStatementBody != null && !(forStatementBody instanceof PsiEmptyStatement)) {
                codeBlock.add(forStatementBody);
            }
            loopBody = codeBlock;
        }
        final PsiStatement update = forStatement.getUpdate();
        if (update != null) {
            final PsiStatement[] updateStatements;
            if (update instanceof PsiExpressionListStatement) {
                final PsiExpressionListStatement expressionListStatement = (PsiExpressionListStatement) update;
                final PsiExpressionList expressionList = expressionListStatement.getExpressionList();
                final PsiExpression[] expressions = expressionList.getExpressions();
                updateStatements = new PsiStatement[expressions.length];
                for (int i = 0; i < expressions.length; i++) {
                    updateStatements[i] = factory.createStatementFromText(expressions[i].getText() + ';',
                            forStatement);
                }
            } else {
                final PsiStatement updateStatement = factory.createStatementFromText(update.getText() + ';',
                        forStatement);
                updateStatements = new PsiStatement[]{updateStatement};
            }
            final Collection<PsiContinueStatement> continueStatements =
                    PsiTreeUtil.findChildrenOfType(loopBody, PsiContinueStatement.class);
            for (PsiContinueStatement continueStatement : continueStatements) {
                BlockUtils.addBefore(continueStatement, updateStatements);
            }
            for (PsiStatement updateStatement : updateStatements) {
                loopBody.addBefore(updateStatement, loopBody.getLastChild());
            }
        }
        if (initialization == null || initialization instanceof PsiEmptyStatement) {
            forStatement.replace(whileStatement);
        } else {
            initialization = (PsiStatement) initialization.copy();
            final PsiStatement newStatement = (PsiStatement) forStatement.replace(whileStatement);
            BlockUtils.addBefore(newStatement, initialization);
        }
    }
}
