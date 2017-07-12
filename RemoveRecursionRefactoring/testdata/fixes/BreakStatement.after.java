import java.util.ArrayList;
import java.util.List;

public class BreakStatement {
    static void breakStatement(int iter, List<Integer> list) {
        List<BreakStatementFrame> stack = new ArrayList<>(); stack.add(new BreakStatementFrame(iter, list));
        while (true) {
            BreakStatementFrame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.iter == 0 ? 1 : 2;
                    break;
                }
                case 1: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                    frame.count = frame.iter;
                    frame.block = 3;
                    break;
                }
                case 3: {
                    frame.block = true ? 4 : 5;
                    break;
                }
                case 4: {
                    frame.list.add(frame.iter);
                    frame.count--;
                    frame.block = frame.count == 0 ? 6 : 7;
                    break;
                }
                case 5: {
                    stack.add(new BreakStatementFrame(frame.iter - 1, frame.list));
                    frame.block = 8;
                    break;
                }
                case 6: {
                    frame.block = 5;
                    break;
                }
                case 7: {
                    frame.block = 3;
                    break;
                }
                case 8: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class BreakStatementFrame {
        int iter;
        List<Integer> list;
        int block;
        int count;

        private BreakStatementFrame(int iter, List<Integer> list) {
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
