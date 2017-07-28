package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.ig.psiutils.BlockUtils;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;

class Refactorings {
  /**
   * @see com.siyeh.ipp.forloop.ReplaceForLoopWithWhileLoopIntention#processIntention(PsiElement)
   */
  static void replaceForStatementWithWhileStatement(PsiForStatement forStatement) {
    PsiStatement initialization = forStatement.getInitialization();
    final PsiElementFactory factory = Util.getFactory(forStatement);
    final PsiWhileStatement whileStatement = (PsiWhileStatement)
      factory.createStatementFromText("while(true) {}", forStatement);
    final PsiExpression forCondition = forStatement.getCondition();
    final PsiExpression whileCondition = whileStatement.getCondition();
    if (forCondition != null) {
      assert whileCondition != null;
      whileCondition.replace(forCondition);
    }
    final PsiBlockStatement blockStatement = (PsiBlockStatement)whileStatement.getBody();
    if (blockStatement == null) {
      return;
    }
    final PsiStatement forStatementBody = forStatement.getBody();
    final PsiElement loopBody;
    if (forStatementBody instanceof PsiBlockStatement) {
      final PsiBlockStatement newWhileBody = (PsiBlockStatement)blockStatement.replace(forStatementBody);
      loopBody = newWhileBody.getCodeBlock();
    }
    else {
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
        final PsiExpressionListStatement expressionListStatement = (PsiExpressionListStatement)update;
        final PsiExpressionList expressionList = expressionListStatement.getExpressionList();
        final PsiExpression[] expressions = expressionList.getExpressions();
        updateStatements = new PsiStatement[expressions.length];
        for (int i = 0; i < expressions.length; i++) {
          updateStatements[i] = factory.createStatementFromText(expressions[i].getText() + ';',
                                                                forStatement);
        }
      }
      else {
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
    }
    else {
      initialization = (PsiStatement)initialization.copy();
      final PsiStatement newStatement = (PsiStatement)forStatement.replace(whileStatement);
      BlockUtils.addBefore(newStatement, initialization);
    }
  }

  /**
   * @see com.siyeh.ipp.forloop.ReplaceForEachLoopWithIteratorForLoopIntention#processIntention(PsiElement)
   */
  static void replaceForEachStatementWithIteratorForLoopStatement(PsiForeachStatement statement, PsiMethod method) {
    final PsiExpression iteratedValue = statement.getIteratedValue();
    if (iteratedValue == null) {
      return;
    }
    final PsiType iteratedValueType = iteratedValue.getType();
    if (!(iteratedValueType instanceof PsiClassType)) {
      return;
    }
    @NonNls final StringBuilder methodCall = new StringBuilder();
    if (ParenthesesUtils.getPrecedence(iteratedValue) > ParenthesesUtils.METHOD_CALL_PRECEDENCE) {
      methodCall.append('(').append(iteratedValue.getText()).append(')');
    }
    else {
      methodCall.append(iteratedValue.getText());
    }
    methodCall.append(".iterator()");
    final Project project = statement.getProject();
    final PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
    final PsiExpression iteratorCall = factory.createExpressionFromText(methodCall.toString(), iteratedValue);
    final PsiType variableType = GenericsUtil.getVariableTypeByExpressionType(iteratorCall.getType());
    if (variableType == null) {
      return;
    }
    @NonNls final StringBuilder newStatement = new StringBuilder();
    newStatement.append("for(").append(variableType.getCanonicalText()).append(' ');
    final JavaCodeStyleManager codeStyleManager = JavaCodeStyleManager.getInstance(project);
    final String iterator = codeStyleManager.suggestUniqueVariableName("iterator", method, true);
    newStatement.append(iterator).append("=").append(iteratorCall.getText()).append(';');
    newStatement.append(iterator).append(".hasNext();) {");
    final CodeStyleSettings codeStyleSettings = CodeStyleSettingsManager.getSettings(project);
    if (codeStyleSettings.GENERATE_FINAL_LOCALS) {
      newStatement.append("final ");
    }
    final PsiParameter iterationParameter = statement.getIterationParameter();
    final PsiType parameterType = iterationParameter.getType();
    final String typeText = parameterType.getCanonicalText();
    newStatement.append(typeText).append(' ').append(iterationParameter.getName()).append(" = ").append(iterator).append(".next();");
    final PsiStatement body = statement.getBody();
    if (body == null) {
      return;
    }
    if (body instanceof PsiBlockStatement) {
      final PsiCodeBlock block = ((PsiBlockStatement)body).getCodeBlock();
      final PsiElement[] children = block.getChildren();
      for (int i = 1; i < children.length - 1; i++) {
        //skip the braces
        newStatement.append(children[i].getText());
      }
    }
    else {
      newStatement.append(body.getText());
    }
    newStatement.append('}');
    PsiReplacementUtil.replaceStatementAndShortenClassNames(statement, newStatement.toString());
  }
}
