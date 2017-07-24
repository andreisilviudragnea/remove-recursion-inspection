package ro.pub.cs.diploma.ir;

public interface Visitor {
  void visit(Block block);

  void visit(ConditionalJumpStatement conditionalJumpStatement);

  void visit(NormalStatement normalStatement);

  void visit(ReturnStatement returnStatement);

  void visit(UnconditionalJumpStatement unconditionalJumpStatement);
}
