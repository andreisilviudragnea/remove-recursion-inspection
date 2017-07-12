import java.util.ArrayList;
import java.util.List;

class Factorial2 {
    static int factorial2(int n) {
        List<Factorial2Frame> stack = new ArrayList<>();
        stack.add(new Factorial2Frame(n));
        int ret = 0;
        while (true) {
            Factorial2Frame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.n == 0 ? 1 : 2;
                    break;
                }
                case 1: {
                    ret = 1;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                    stack.add(new Factorial2Frame(frame.n - 1));
                    frame.block = 3;
                    break;
                }
                case 3: {
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

    private static class Factorial2Frame {
        int n;
        int block;
        int temp;

        private Factorial2Frame(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Factorial2.factorial2(12));
    }
}
