import java.util.ArrayList;
import java.util.List;

class Dependency1 {
    public int factorial(int val) {
        return factorial(val, 1);
    }

    private int factorial(int val, int runningVal) {
        List<FactorialFrame> stack = new ArrayList<>();
        stack.add(new FactorialFrame(val, runningVal));
        int ret = 0;
        while (true) {
            FactorialFrame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.val == 1 ? 1 : 3;
                    break;
                }
                case 1: {
                    ret = frame.runningVal;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                }
                case 3: {
                    stack.add(new FactorialFrame(frame.val - 1, frame.runningVal * frame.val));
                    frame.block = 4;
                    break;
                }
                case 4: {
                    frame.temp = ret;
                    ret = (frame.temp);
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class FactorialFrame {
        int val;
        int runningVal;
        int block;
        int temp;

        private FactorialFrame(int val, int runningVal) {
            this.val = val;
            this.runningVal = runningVal;
        }
    }
}