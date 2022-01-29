package ro.pub.cs.diploma.ir

interface Statement {
    fun accept(visitor: Visitor)
}
