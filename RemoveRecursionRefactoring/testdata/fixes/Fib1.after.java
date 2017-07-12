import java.util.ArrayList;
import java.util.List;

public class Fib1 {
    static int fib1(int n) {
        List<Fib1Frame> stack = new ArrayList<>();
        stack.add(new Fib1Frame(n));
        int ret = 0;
        while (true) {
            Fib1Frame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.n == 0 ? 1 : 2;
                    break;
                }
                case 1: {
                    ret = 0;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                    frame.block = frame.n == 1 ? 3 : 4;
                    break;
                }
                case 3: {
                    ret = 1;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 4: {
                    stack.add(new Fib1Frame(frame.n - 1));
                    frame.block = 5;
                    break;
                }
                case 5: {
                    frame.temp = ret;
                    stack.add(new Fib1Frame(frame.n - 2));
                    frame.block = 6;
                    break;
                }
                case 6: {
                    frame.temp1 = ret;
                    ret = frame.temp + frame.temp1;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class Fib1Frame {
        int n;
        int block;
        int temp;
        int temp1;

        private Fib1Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Fib1.fib1(25));
    }
}
