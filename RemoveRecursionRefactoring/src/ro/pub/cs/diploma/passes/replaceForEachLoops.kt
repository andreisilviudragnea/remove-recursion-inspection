package ro.pub.cs.diploma.passes

import com.intellij.psi.PsiMethod
import ro.pub.cs.diploma.Refactorings
import ro.pub.cs.diploma.getPsiForEachStatements

fun replaceForEachLoopsWithIteratorForLoops(method: PsiMethod) {
  method.getPsiForEachStatements().forEach { statement -> Refactorings.replaceForEachLoopWithIteratorForLoop(statement, method) }
}

fun replaceForEachLoopsWithIndexedForLoops(method: PsiMethod) {
  method.getPsiForEachStatements().forEach { statement -> Refactorings.replaceForEachLoopWithIndexedForLoop(statement, method) }
}
