import java.util.Deque;

public class Fib2 {
    static int fib2(int n) {
        Deque<Fib2Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new Fib2Frame(n));
        int ret = 0;
        while (!stack.isEmpty()) {
            Fib2Frame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        ret = 0;
                        stack.pop();
                        break;
                    } else {
                        if (frame.n == 1) {
                            ret = 1;
                            stack.pop();
                            break;
                        } else {
                            stack.push(new Fib2Frame(frame.n - 1));
                            frame.block = 3;
                            break;
                        }
                    }
                }
                case 1: {
                }
                case 2: {
                    frame.block = 1;
                    break;
                }
                case 3: {
                    frame.temp = ret;
                    stack.push(new Fib2Frame(frame.n - 2));
                    frame.block = 4;
                    break;
                }
                case 4: {
                    frame.temp1 = ret;
                    ret = frame.temp + frame.temp1;
                    stack.pop();
                    break;
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
