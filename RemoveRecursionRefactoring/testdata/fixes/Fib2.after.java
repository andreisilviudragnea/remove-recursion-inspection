import java.util.ArrayList;
import java.util.List;

public class Fib2 {
    static int fib2(int n) {
        List<Fib2Frame> stack = new ArrayList<>();
        stack.add(new Fib2Frame(n));
        int ret = 0;
        while (true) {
            Fib2Frame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.n == 0 ? 1 : 3;
                    break;
                }
                case 1: {
                    ret = 0;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 3: {
                    frame.block = frame.n == 1 ? 4 : 6;
                    break;
                }
                case 4: {
                    ret = 1;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 6: {
                    stack.add(new Fib2Frame(frame.n - 1));
                    frame.block = 7;
                    break;
                }
                case 7: {
                    frame.temp = ret;
                    stack.add(new Fib2Frame(frame.n - 2));
                    frame.block = 8;
                    break;
                }
                case 8: {
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

    private static class Fib2Frame {
        int n;
        int block;
        int temp;
        int temp1;

        private Fib2Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Fib2.fib2(25));
    }
}
