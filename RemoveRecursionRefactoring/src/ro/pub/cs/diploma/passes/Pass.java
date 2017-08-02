package ro.pub.cs.diploma.passes;

public interface Pass<T, S, R> {
  S collect(T t);

  R transform(S s);

  default R apply(T t) {
    return transform(collect(t));
  }
}
