package ro.pub.cs.diploma.ir;

public interface Statement {
  void accept(Visitor visitor);
}
