import java.util.ArrayList;
import java.util.List;

public class Factorial1 {
    static int factorial1(int n) {
        List<Factorial1Context> stack = new ArrayList<>();
        stack.add(new Factorial1Context(n));
        int ret = 0;
        while (true) {
            Factorial1Context context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.n == 0 ? 1 : 3;
                    break;
                }
                case 1: {
                    ret = 1;
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
                    stack.add(new Factorial1Context(context.n - 1));
                    break;
                }
                case 4: {
                    context.temp0 = ret;
                    ret = context.n * context.temp0;
                    if (stack.size() == 1)
                        return ret;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class Factorial1Context {
        int n;
        int section;
        int temp0;

        private Factorial1Context(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Factorial1.factorial1(12));
    }
}
