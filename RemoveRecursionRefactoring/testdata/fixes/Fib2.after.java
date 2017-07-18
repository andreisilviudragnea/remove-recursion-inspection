import java.util.Deque;

public class Fib2 {
    static int fib2(int n) {
        Deque<Fib2Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new Fib2Frame(n));
        int ret = 0;
        while (!stack.isEmpty()) {
            Fib2Frame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    frame.block = frame.n == 0 ? 1 : 3;
                    break switchLabel;
                }
                case 1: {
                    ret = 0;
                    stack.pop();
                    break switchLabel;
                }
                case 3: {
                    frame.block = frame.n == 1 ? 4 : 6;
                    break switchLabel;
                }
                case 4: {
                    ret = 1;
                    stack.pop();
                    break switchLabel;
                }
                case 6: {
                    stack.push(new Fib2Frame(frame.n - 1));
                    frame.block = 7;
                    break switchLabel;
                }
                case 7: {
                    frame.temp = ret;
                    stack.push(new Fib2Frame(frame.n - 2));
                    frame.block = 8;
                    break switchLabel;
                }
                case 8: {
                    frame.temp1 = ret;
                    ret = frame.temp + frame.temp1;
                    stack.pop();
                    break switchLabel;
                }
            }
        }
        return ret;
    }

    private static class Fib2Frame {
        private int n;
        private int temp;
        private int temp1;
        private int block;

        private Fib2Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Fib2.fib2(25));
    }
}
