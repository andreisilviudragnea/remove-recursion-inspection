import java.util.ArrayList;
import java.util.List;

class Factorial2 {
    static int factorial2(int n) {
        List<Factorial2Context> stack = new ArrayList<>();
        stack.add(new Factorial2Context(n));
        int ret = 0;
        while (true) {
            Factorial2Context context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.n == 0 ? 1 : 2;
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
                    context.section = 3;
                    stack.add(new Factorial2Context(context.n - 1));
                    break;
                }
                case 3: {
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

    private static class Factorial2Context {
        int n;
        int section;
        int temp0;

        private Factorial2Context(int n) {
            this.n = n;
        }
    }

    public static void main(String[] args) {
        System.out.println(Factorial2.factorial2(12));
    }
}
