package ro.pub.cs.diploma.passes

import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethod
import ro.pub.cs.diploma.Refactorings
import ro.pub.cs.diploma.getPsiForEachStatements

class ReplaceForEachLoopsWithIndexedForLoops(private val myMethod: PsiMethod) : Pass<PsiMethod, List<PsiForeachStatement>, Any> {

  override fun collect(method: PsiMethod): List<PsiForeachStatement> = method.getPsiForEachStatements()

  override fun transform(statements: List<PsiForeachStatement>): Any? {
    statements.forEach { statement -> Refactorings.replaceForEachLoopWithIndexedForLoop(statement, myMethod) }
    return null
  }
}
