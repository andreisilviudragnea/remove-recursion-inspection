import java.util.ArrayDeque;
import java.util.Deque;

public class FibSwitchFallThrough {
  static int counter;
  static int fib(int n) {
      final Deque<FibFrame> stack = new ArrayDeque<>();
      stack.push(new FibFrame(n));
      int ret = 0;
      while (!stack.isEmpty()) {
          final FibFrame frame = stack.peek();
          switch (frame.block) {
              case 0: {
                  switch (frame.n) {
                      case 0:
                          counter++;
                          frame.block = 3;
                          break;
                      case 1:
                          frame.block = 3;
                          break;
                      default:
                          stack.push(new FibFrame(frame.n - 1));
                          frame.block = 5;
                          break;
                  }
                  break;
              }
              case 1: {
                  ret = frame.ret;
                  stack.pop();
                  break;
              }
              case 3: {
                  frame.ret = 1;
                  frame.block = 1;
                  break;
              }
              case 5: {
                  frame.temp = ret;
                  stack.push(new FibFrame(frame.n - 2));
                  frame.block = 6;
                  break;
              }
              case 6: {
                  int temp1 = ret;
                  frame.ret = frame.temp + temp1;
                  frame.block = 1;
                  break;
              }
          }
      }
      return ret;
  }

    private static class FibFrame {
        private int n;
        private int ret;
        private int temp;
        private int block;

        private FibFrame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
    System.out.println(FibSwitchFallThrough.fib(25));
    System.out.println(counter);
  }
}
