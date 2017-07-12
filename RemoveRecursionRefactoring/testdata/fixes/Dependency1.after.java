import java.util.ArrayList;
import java.util.List;

class Dependency1 {
    public int factorial(int val) {
        return factorial(val, 1);
    }

    private int factorial(int val, int runningVal) {
        List<FactorialContext> stack = new ArrayList<>();
        stack.add(new FactorialContext(val, runningVal));
        int ret = 0;
        while (true) {
            FactorialContext context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.val == 1 ? 1 : 3;
                    break;
                }
                case 1: {
                    ret = context.runningVal;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                }
                case 3: {
                    context.section = 4;
                    stack.add(new FactorialContext(context.val - 1, context.runningVal * context.val));
                    break;
                }
                case 4: {
                    context.temp0 = ret;
                    ret = (context.temp0);
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class FactorialContext {
        int val;
        int runningVal;
        int section;
        int temp0;

        private FactorialContext(int val, int runningVal) {
            this.val = val;
            this.runningVal = runningVal;
        }
    }
}