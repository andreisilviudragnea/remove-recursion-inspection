package ro.pub.cs.diploma;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.ig.psiutils.ParenthesesUtils;
import org.jetbrains.annotations.NonNls;

class Refactorings {

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
