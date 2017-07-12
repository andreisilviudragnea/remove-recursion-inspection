import java.util.ArrayList;
import java.util.List;

public class BreakStatement {
    static void breakStatement(int iter, List<Integer> list) {
        List<BreakStatementContext> stack = new ArrayList<>(); stack.add(new BreakStatementContext(iter, list));
        while (true) {
            BreakStatementContext context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.iter == 0 ? 1 : 2;
                    break;
                }
                case 1: {
                    if (stack.size() == 1)
                        return;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                    context.count = context.iter;
                    context.section = 3;
                    break;
                }
                case 3: {
                    context.section = true ? 4 : 5;
                    break;
                }
                case 4: {
                    context.list.add(context.iter);
                    context.count--;
                    context.section = context.count == 0 ? 6 : 7;
                    break;
                }
                case 5: {
                    context.section = 8;
                    stack.add(new BreakStatementContext(context.iter - 1, context.list));
                    break;
                }
                case 6: {
                    context.section = 5;
                    break;
                }
                case 7: {
                    context.section = 3;
                    break;
                }
                case 8: {
                    if (stack.size() == 1)
                        return;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class BreakStatementContext {
        int iter;
        List<Integer> list;
        int section;
        int count;

        private BreakStatementContext(int iter, List<Integer> list) {
            this.iter = iter;
            this.list = list;
        }
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        BreakStatement.breakStatement(4, list);
        System.out.println(list);
    }
}
