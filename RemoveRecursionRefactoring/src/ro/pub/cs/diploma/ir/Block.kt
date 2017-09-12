package ro.pub.cs.diploma.ir

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiStatement

class Block(val id: Int) : Statement {
  private val myStatements = ArrayList<Statement>()
  private val myInBlocks = ArrayList<Ref<Block>>()
  private val myOutBlocks = ArrayList<Ref<Block>>()

  private var doNotInline: Boolean = false
  var isFinished: Boolean = false
    private set

  val isInlinable: Boolean
    get() = myInBlocks.size == 1 && !doNotInline

  val outBlocks: List<Ref<Block>>
    get() = myOutBlocks

  internal val statements: List<Statement>
    get() = myStatements

  fun addConditionalJump(condition: PsiExpression,
                         thenBlockRef: Ref<Block>,
                         elseBlockRef: Ref<Block>) {
    if (isFinished) {
      return
    }
    myStatements.add(ConditionalJumpStatement(condition, thenBlockRef, elseBlockRef))
    addEdgeTo(thenBlockRef)
    addEdgeTo(elseBlockRef)
    isFinished = true
  }

  fun addUnconditionalJump(blockRef: Ref<Block>) {
    if (isFinished) {
      return
    }
    myStatements.add(UnconditionalJumpStatement(blockRef))
    addEdgeTo(blockRef)
    isFinished = true
  }

  fun addReturnStatement(statement: PsiReturnStatement) {
    myStatements.add(ReturnStatement(statement))
    isFinished = true
  }

  fun addSwitchStatement(expression: PsiExpression, statements: List<Statement>) {
    myStatements.add(SwitchStatement(expression, statements))
    statements.filterIsInstance<UnconditionalJumpStatement>().forEach { addEdgeTo(it.blockRef) }
    isFinished = true
  }

  fun add(statement: PsiStatement) {
    myStatements.add(NormalStatement(statement))
  }

  fun addEdgeTo(blockRef: Ref<Block>) {
    myOutBlocks.add(blockRef)
    blockRef.get().myInBlocks.add(blockRef)
  }

  fun setDoNotInline(doNotInline: Boolean) {
    this.doNotInline = doNotInline
  }

  fun inlineIfTrivial(): Boolean {
    if (id != 0 && myStatements.size == 1 && myStatements[0] is UnconditionalJumpStatement) {
      val jumpBlock = (myStatements[0] as UnconditionalJumpStatement).block
      for (inBlock in myInBlocks) {
        inBlock.set(jumpBlock)
      }
      if (doNotInline) {
        jumpBlock.doNotInline = true
      }
      return false
    }
    return true
  }

  override fun accept(visitor: Visitor) {
    visitor.visit(this)
  }

  fun toDot(): List<String> {
    val strings = ArrayList<String>()
    if (myStatements.size > 0) {
      myStatements.subList(0, myStatements.size - 1).mapTo(strings) {
        (it as WrapperStatement).statement.text
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("<", "\\<")
            .replace(">", "\\>")
            .replace("\n", "\\n")
      }
    }

    val strings2 = ArrayList<String>()
    strings2.add("id: $id")
    strings2.add(strings.joinToString("\\n"))
    val statements = ArrayList<String>()
    if (myStatements.size > 0) {
      val lastStatement = myStatements.last()
      when (lastStatement) {
        is ConditionalJumpStatement -> {
          strings2.add(lastStatement.condition.text
              .replace("{", "\\{")
              .replace("}", "\\}")
              .replace("<", "\\<")
              .replace(">", "\\>")
              .replace("\n", "\\n"))
          strings2.add("{<true>true|<false>false}")
          statements.add("$id:true -> ${lastStatement.thenBlock.id};")
          statements.add("$id:false -> ${lastStatement.elseBlock.id};")
        }
        is UnconditionalJumpStatement -> {
          val jumpId = lastStatement.block.id
          strings2.add("jump $jumpId;")
          statements.add("$id -> $jumpId;")
        }
        is ReturnStatement -> strings2.add(lastStatement.statement.text
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("<", "\\<")
            .replace(">", "\\>")
            .replace("\n", "\\n"))
      }
    }
    statements.add("$id [label=\"{${strings2.joinToString("|")}}\" ${if (isInlinable) "" else "color=red"}];")

    return statements
  }
}
