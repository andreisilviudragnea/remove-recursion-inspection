import java.util.ArrayList;
import java.util.List;

public class LabeledBreakStatement {
    static void labeledBreakStatement(int iter, List<Integer> list) {
        List<LabeledBreakStatementContext> stack = new ArrayList<>();
        stack.add(new LabeledBreakStatementContext(iter, list));
        while (true) {
            LabeledBreakStatementContext context = stack.get(stack.size() - 1);
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
                    context.section = 6;
                    break;
                }
                case 5: {
                    context.section = 11;
                    stack.add(new LabeledBreakStatementContext(context.iter - 1, context.list));
                    break;
                }
                case 6: {
                    context.section = true ? 7 : 8;
                    break;
                }
                case 7: {
                    context.list.add(context.iter);
                    context.count--;
                    context.section = context.count == 0 ? 9 : 10;
                    break;
                }
                case 8: {
                    context.section = 3;
                    break;
                }
                case 9: {
                    context.section = 5;
                    break;
                }
                case 10: {
                    context.section = 6;
                    break;
                }
                case 11: {
                    if (stack.size() == 1)
                        return;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class LabeledBreakStatementContext {
        int iter;
        List<Integer> list;
        int section;
        int count;

        private LabeledBreakStatementContext(int iter, List<Integer> list) {
            this.iter = iter;
            this.list = list;
        }
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        LabeledBreakStatement.labeledBreakStatement(4, list);
        System.out.println(list);
    }
}
