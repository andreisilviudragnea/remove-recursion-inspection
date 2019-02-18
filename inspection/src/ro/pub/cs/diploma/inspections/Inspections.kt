package ro.pub.cs.diploma.inspections

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix
import org.jetbrains.annotations.Nls
import ro.pub.cs.diploma.NameManager
import ro.pub.cs.diploma.RemoveRecursionBundle
import ro.pub.cs.diploma.createIterativeBody
import ro.pub.cs.diploma.getContainingMethod

abstract class DummyInspection : BaseInspection() {
  protected abstract val key: String
  protected abstract val steps: Int

  @Nls
  override fun getDisplayName() = RemoveRecursionBundle.message(key)

  override fun buildErrorString(vararg infos: Any) = RemoveRecursionBundle.message(key)

  override fun buildVisitor(): BaseInspectionVisitor {
    return object : BaseInspectionVisitor() {
      override fun visitMethod(method: PsiMethod) {
        super.visitMethod(method)
        registerMethodError(method)
      }
    }
  }

  override fun buildFix(vararg infos: Any): InspectionGadgetsFix? {
    return object : InspectionGadgetsFix() {
      override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val method = descriptor.psiElement.getContainingMethod() ?: return
        createIterativeBody(13 - steps, method, NameManager(method)).take(steps).toList()
      }

      @Nls
      override fun getFamilyName() = RemoveRecursionBundle.message(key)
    }
  }
}

class AddFrameClassInspection : DummyInspection() {
  override val key get() = "add.frame.class"
  override val steps get() = 5
}

class ExtractRecursiveCallsToStatementsInspection : DummyInspection() {
  override val key get() = "extract.recursive.calls.to.statements"
  override val steps get() = 4
}

class GenerateCFGInspection : DummyInspection() {
  override val key get() = "generate.cfg"
  override val steps get() = 9
}

class IncorporateBodyInspection : DummyInspection() {
  override val key get() = "incorporate.body"
  override val steps get() = 6
}

class InlineBlocksInspection : DummyInspection() {
  override val key: String get() = "inline.blocks"
  override val steps: Int get() = 12
}

class InlineTrivialBlocksInspection : DummyInspection() {
  override val key get() = "inline.trivial.blocks"
  override val steps get() = 11
}

class RemoveUnreachableBlocksInspection : DummyInspection() {
  override val key get() = "remove.unreachable.blocks"
  override val steps get() = 10
}

class RenameVariablesToUniqueNamesInspection : DummyInspection() {
  override val key get() = "rename.variables.to.unique.names"
  override val steps get() = 2
}

class ReplaceDeclarationsHavingInitializersWithAssignmentsInspection : DummyInspection() {
  override val key get() = "replace.declarations.having.initializers.with.assignments"
  override val steps get() = 8
}

class ReplaceForEachLoopsWithForLoopsInspection : DummyInspection() {
  override val key get() = "replace.foreach.loops.with.for.loops"
  override val steps get() = 3
}

class ReplaceIdentifierWithFrameAccessInspection : DummyInspection() {
  override val key get() = "replace.identifier.with.frame.access"
  override val steps get() = 7
}

class ReplaceReturnStatementsInspection : DummyInspection() {
  override val key get() = "replace.return.statements"
  override val steps get() = 13
}

class ReplaceSingleStatementsWithBlockStatementsInspection : DummyInspection() {
  override val key get() = "replace.single.statements.with.block.statements"
  override val steps get() = 1
}
