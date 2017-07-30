package ro.pub.cs.diploma.ir;

import org.jetbrains.annotations.NotNull;

public interface Statement {
  void accept(@NotNull final Visitor visitor);
}
