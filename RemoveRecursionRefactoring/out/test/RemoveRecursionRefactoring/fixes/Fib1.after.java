import java.util.Deque;

public class Fib1 {
    static int fib1(int n) {
        Deque<Fib1Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new Fib1Frame(n));
        int ret = 0;
        while (!stack.isEmpty()) {
            Fib1Frame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        ret = 0;
                        stack.pop();
                        break;
                    }
                    if (frame.n == 1) {
                        ret = 1;
                        stack.pop();
                        break;
                    }
                    stack.push(new Fib1Frame(frame.n - 1));
                    frame.block = 1;
                    break;
                }
                case 1: {
                    frame.temp = ret;
                    stack.push(new Fib1Frame(frame.n - 2));
                    frame.block = 2;
                    break;
                }
                case 2: {
                    frame.temp1 = ret;
                    ret = frame.temp + frame.temp1;
                    stack.pop();
                    break;
                }
            }
        }
        return ret;
    }

    private static class Fib1Frame {
        private int n;
        private int temp;
        private int temp1;
        private int block;

        private Fib1Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Fib1.fib1(25));
    }
}
