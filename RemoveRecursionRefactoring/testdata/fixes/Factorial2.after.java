import java.util.Deque;

class Factorial2 {
    static int factorial2(int n) {
        Deque<Factorial2Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new Factorial2Frame(n));
        int ret = 0;
        while (!stack.isEmpty()) {
            Factorial2Frame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        ret = 1;
                        stack.pop();
                        break switchLabel;
                    }
                    stack.push(new Factorial2Frame(frame.n - 1));
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    frame.temp = ret;
                    ret = frame.n * frame.temp;
                    stack.pop();
                    break switchLabel;
                }
            }
        }
        return ret;
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
