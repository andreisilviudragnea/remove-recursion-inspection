import java.util.ArrayList;
import java.util.List;

public class Fib1 {
    static int fib1(int n) {
        List<Fib1Context> stack = new ArrayList<>();
        stack.add(new Fib1Context(n));
        int ret = 0;
        while (true) {
            Fib1Context context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.n == 0 ? 1 : 2;
                    break;
                }
                case 1: {
                    ret = 0;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                    context.section = context.n == 1 ? 3 : 4;
                    break;
                }
                case 3: {
                    ret = 1;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 4: {
                    context.section = 5;
                    stack.add(new Fib1Context(context.n - 1));
                    break;
                }
                case 5: {
                    context.temp0 = ret;
                    context.section = 6;
                    stack.add(new Fib1Context(context.n - 2));
                    break;
                }
                case 6: {
                    context.temp1 = ret;
                    ret = context.temp0 + context.temp1;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class Fib1Context {
        int n;
        int section;
        int temp0;
        int temp1;

        private Fib1Context(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Fib1.fib1(25));
    }
}
