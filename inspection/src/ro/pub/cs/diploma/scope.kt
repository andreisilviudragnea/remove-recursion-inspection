package ro.pub.cs.diploma

import com.intellij.psi.*

fun PsiParameter.getElementsInScope(): List<PsiElement> {
  val elements = ArrayList<PsiElement>()
  when (parent) {
    is PsiParameterList -> (parent.parent as PsiMethod).body?.let { elements.add(it) }
    is PsiForeachStatement -> (parent as PsiForeachStatement).body?.let { elements.add(it) }
  }
  return elements
}

fun PsiLocalVariable.getElementsInScope(): List<PsiElement> {
  val declarationStatement = this.parent as PsiDeclarationStatement
  val parent = declarationStatement.parent

  val elements = ArrayList<PsiElement>()

  // This is because the variable is actually used only after it has been initialized.
  //final PsiExpression initializer = variable.getInitializer();
  //if (initializer != null) {
  //  elements.add(initializer);
  //}

  var met = false
  for (element in declarationStatement.declaredElements) {
    if (element is PsiLocalVariable) {
      if (met) {
        elements.add(element)
      }
      if (element === this) {
        met = true
      }
    }
  }

  when (parent) {
    is PsiForStatement -> {
      parent.condition?.let { elements.add(it) }
      parent.update?.let { elements.add(it) }
      parent.body?.let { elements.add(it) }
    }
    is PsiCodeBlock -> {
      met = false
      for (psiStatement in parent.statements) {
        if (met) {
          elements.add(psiStatement)
        }
        if (psiStatement === declarationStatement) {
          met = true
        }
      }
    }
  }

  return elements
}