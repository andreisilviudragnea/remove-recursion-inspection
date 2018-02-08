public class FibSwitchFallThrough {
  static int counter;
  static int fib(int n) {
    int ret;
    switch (n) {
      case 0:
        counter++;
      case 1:
        ret = 1;
        break;
      default:
        ret = fib(n - 1) + fib(n - 2);
    }
    return ret;
  }

  public static void main(String[] args) {
    System.out.println(FibSwitchFallThrough.fib(25));
    System.out.println(counter);
  }
}
