import java.util.ArrayList;
import java.util.List;

public class Fib1 {
    static int fib1(int n) {
        List<Fib1Frame> stack = new ArrayList<>();
        stack.add(new Fib1Frame(n));
        int ret = 0;
        while (true) {
            Fib1Frame frame = stack.get(stack.size() - 1);
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        ret = 0;
                        if (stack.size() == 1)
                            return ret;
                        stack.remove(stack.size() - 1);
                        break switchLabel;
                    }
                    if (frame.n == 1) {
                        ret = 1;
                        if (stack.size() == 1)
                            return ret;
                        stack.remove(stack.size() - 1);
                        break switchLabel;
                    }
                    stack.add(new Fib1Frame(frame.n - 1));
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    frame.temp = ret;
                    stack.add(new Fib1Frame(frame.n - 2));
                    frame.block = 2;
                    break switchLabel;
                }
                case 2: {
                    frame.temp1 = ret;
                    ret = frame.temp + frame.temp1;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break switchLabel;
                }
            }
        }
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
