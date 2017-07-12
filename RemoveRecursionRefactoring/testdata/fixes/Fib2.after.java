import java.util.ArrayList;
import java.util.List;

public class Fib2 {
    static int fib2(int n) {
        List<Fib2Context> stack = new ArrayList<>();
        stack.add(new Fib2Context(n));
        int ret = 0;
        while (true) {
            Fib2Context context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.n == 0 ? 1 : 3;
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
                }
                case 3: {
                    context.section = context.n == 1 ? 4 : 6;
                    break;
                }
                case 4: {
                    ret = 1;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 5: {
                    context.section = 2;
                    break;
                }
                case 6: {
                    context.section = 7;
                    stack.add(new Fib2Context(context.n - 1));
                    break;
                }
                case 7: {
                    context.temp0 = ret;
                    context.section = 8;
                    stack.add(new Fib2Context(context.n - 2));
                    break;
                }
                case 8: {
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

    private static class Fib2Context {
        int n;
        int section;
        int temp0;
        int temp1;

        private Fib2Context(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Fib2.fib2(25));
    }
}
