import java.util.ArrayList;
import java.util.List;

class Factorial2 {
    static int factorial2(int n) {
        List<Factorial2Frame> stack = new ArrayList<>();
        stack.add(new Factorial2Frame(n));
        int ret = 0;
        while (true) {
            Factorial2Frame frame = stack.get(stack.size() - 1);
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        ret = 1;
                        if (stack.size() == 1)
                            return ret;
                        stack.remove(stack.size() - 1);
                        break switchLabel;
                    }
                    stack.add(new Factorial2Frame(frame.n - 1));
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    frame.temp = ret;
                    ret = frame.n * frame.temp;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break switchLabel;
                }
            }
        }
    }

    private static class Factorial2Frame {
        private int n;
        private int temp;
        private int block;

        private Factorial2Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Factorial2.factorial2(12));
    }
}
