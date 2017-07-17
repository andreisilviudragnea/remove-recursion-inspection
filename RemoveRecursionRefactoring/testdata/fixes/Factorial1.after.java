import java.util.ArrayList;
import java.util.List;

public class Factorial1 {
    static int factorial1(int n) {
        List<Factorial1Frame> stack = new ArrayList<>();
        stack.add(new Factorial1Frame(n));
        int ret = 0;
        while (true) {
            Factorial1Frame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.n == 0 ? 1 : 3;
                    break;
                }
                case 1: {
                    ret = 1;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 3: {
                    stack.add(new Factorial1Frame(frame.n - 1));
                    frame.block = 4;
                    break;
                }
                case 4: {
                    frame.temp = ret;
                    ret = frame.n * frame.temp;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
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
