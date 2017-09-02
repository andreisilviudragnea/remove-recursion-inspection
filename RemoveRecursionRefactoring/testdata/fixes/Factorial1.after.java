import java.util.ArrayDeque;
import java.util.Deque;

public class Factorial1 {
    static int factorial1(int n) {
        final Deque<Factorial1Frame> stack = new ArrayDeque<>();
        stack.push(new Factorial1Frame(n));
        int ret = 0;
        while (!stack.isEmpty()) {
            final Factorial1Frame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        ret = 1;
                        stack.pop();
                        break;
                    } else {
                        stack.push(new Factorial1Frame(frame.n - 1));
                        frame.block = 4;
                        break;
                    }
                }
                case 4: {
                    int temp = ret;
                    ret = frame.n * temp;
                    stack.pop();
                    break;
                }
            }
        }
        return ret;
    }

    private static class Factorial1Frame {
        private int n;
        private int block;

        private Factorial1Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Factorial1.factorial1(12));
    }
}
