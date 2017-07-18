import java.util.Deque;

class Dependency1 {
    public int factorial(int val) {
        return factorial(val, 1);
    }

    private int factorial(int val, int runningVal) {
        Deque<FactorialFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new FactorialFrame(val, runningVal));
        int ret = 0;
        while (!stack.isEmpty()) {
            FactorialFrame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    frame.block = frame.val == 1 ? 1 : 3;
                    break switchLabel;
                }
                case 1: {
                    ret = frame.runningVal;
                    stack.pop();
                    break switchLabel;
                }
                case 3: {
                    stack.push(new FactorialFrame(frame.val - 1, frame.runningVal * frame.val));
                    frame.block = 4;
                    break switchLabel;
                }
                case 4: {
                    frame.temp = ret;
                    ret = (frame.temp);
                    stack.pop();
                    break switchLabel;
                }
            }
        }
        return ret;
    }

    private static class FactorialFrame {
        private int val;
        private int runningVal;
        private int temp;
        private int block;

        private FactorialFrame(int val, int runningVal) {
            this.val = val;
            this.runningVal = runningVal;
        }
    }
}