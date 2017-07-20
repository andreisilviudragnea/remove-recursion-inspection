import java.util.Deque;

public class Factorial1 {
    static int factorial1(int n) {
        Deque<Factorial1Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new Factorial1Frame(n));
        int ret = 0;
        while (!stack.isEmpty()) {
            Factorial1Frame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    frame.block = frame.n == 0 ? 1 : 3;
                    break;
                }
                case 1: {
                    ret = 1;
                    stack.pop();
                    break;
                }
                case 3: {
                    stack.push(new Factorial1Frame(frame.n - 1));
                    frame.block = 4;
                    break;
                }
                case 4: {
                    frame.temp = ret;
                    ret = frame.n * frame.temp;
                    stack.pop();
                    break;
                }
            }
        }
        return ret;
    }

    private static class Factorial1Frame {
        private int n;
        private int temp;
        private int block;

        private Factorial1Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Factorial1.factorial1(12));
    }
}
