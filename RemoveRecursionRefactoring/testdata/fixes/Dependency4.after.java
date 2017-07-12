import java.util.ArrayList;
import java.util.List;

class Dependency4 {
    int calculate(int one, int two, int three) {
        List<CalculateContext> stack = new ArrayList<>();
        stack.add(new CalculateContext(one, two, three));
        int ret = 0;
        while (true) {
            CalculateContext context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = 1;
                    stack.add(new CalculateContext(context.one ^ 2, context.one * context.two, context.one + context.two + context.three));
                    break;
                }
                case 1: {
                    context.temp0 = ret;
                    ret = context.temp0;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class CalculateContext {
        int one;
        int two;
        int three;
        int section;
        int temp0;

        private CalculateContext(int one, int two, int three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }
    }
}