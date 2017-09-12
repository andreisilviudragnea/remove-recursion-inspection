package ro.pub.cs.diploma.passes

import com.intellij.psi.PsiForeachStatement
import com.intellij.psi.PsiMethod
import ro.pub.cs.diploma.Refactorings
import ro.pub.cs.diploma.getPsiForEachStatements

class ReplaceForEachLoopsWithIteratorForLoops(private val myMethod: PsiMethod) : Pass<PsiMethod, List<PsiForeachStatement>, Nothing?> {

  override fun collect(method: PsiMethod): List<PsiForeachStatement> = method.getPsiForEachStatements()

  override fun transform(statements: List<PsiForeachStatement>): Nothing? {
    statements.forEach { statement -> Refactorings.replaceForEachLoopWithIteratorForLoop(statement, myMethod) }
    return null
  }
}
