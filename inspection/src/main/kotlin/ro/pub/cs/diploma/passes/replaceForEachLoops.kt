package ro.pub.cs.diploma.passes

import com.intellij.psi.PsiMethod
import ro.pub.cs.diploma.getPsiForEachStatements
import ro.pub.cs.diploma.replaceForEachLoopWithIndexedForLoop
import ro.pub.cs.diploma.replaceForEachLoopWithIteratorForLoop

fun replaceForEachLoopsWithIteratorForLoops(method: PsiMethod) {
    method.getPsiForEachStatements().forEach { statement -> replaceForEachLoopWithIteratorForLoop(statement, method) }
}

fun replaceForEachLoopsWithIndexedForLoops(method: PsiMethod) {
    method.getPsiForEachStatements().forEach { statement -> replaceForEachLoopWithIndexedForLoop(statement) }
}
