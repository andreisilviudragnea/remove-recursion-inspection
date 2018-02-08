import java.util.ArrayDeque;
import java.util.Deque;

class Dependency1 {
    public int factorial(int val) {
        return factorial(val, 1);
    }

    private int factorial(int val, int runningVal) {
        final Deque<FactorialFrame> stack = new ArrayDeque<>();
        stack.push(new FactorialFrame(val, runningVal));
        int ret = 0;
        while (!stack.isEmpty()) {
            final FactorialFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.val == 1) {
                        ret = frame.runningVal;
                        stack.pop();
                        break;
                    } else {
                        stack.push(new FactorialFrame(frame.val - 1, frame.runningVal * frame.val));
                        frame.block = 4;
                        break;
                    }
                }
                case 4: {
                    int temp = ret;
                    ret = (temp);
                    stack.pop();
                    break;
                }
            }
        }
        return ret;
    }

    private static class FactorialFrame {
        private int val;
        private int runningVal;
        private int block;

        private FactorialFrame(int val, int runningVal) {
            this.val = val;
            this.runningVal = runningVal;
        }
    }
}