package ro.pub.cs.diploma

import com.intellij.psi.*

fun PsiParameter.getElementsInScope(): List<PsiElement> {
  val parent = this.parent
  val elements = ArrayList<PsiElement>()

  if (parent is PsiParameterList) {
    val body = (parent.getParent() as PsiMethod).body
    if (body != null) {
      elements.add(body)
    }
  }
  else if (parent is PsiForeachStatement) {
    val body = parent.body
    if (body != null) {
      elements.add(body)
    }
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

  when(parent) {
    is PsiForStatement -> {
      val condition = parent.condition
      if (condition != null) {
        elements.add(condition)
      }
      val update = parent.update
      if (update != null) {
        elements.add(update)
      }
      val body = parent.body
      if (body != null) {
        elements.add(body)
      }
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