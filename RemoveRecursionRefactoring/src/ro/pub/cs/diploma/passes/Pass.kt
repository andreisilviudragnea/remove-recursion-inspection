package ro.pub.cs.diploma.passes

interface Pass<in T, S, out R> {
  fun collect(t: T): S

  fun transform(s: S): R

  fun apply(t: T): R = transform(collect(t))
}
