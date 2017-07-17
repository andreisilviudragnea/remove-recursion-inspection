import java.util.ArrayList;
import java.util.List;

public class LabeledBreakStatement {
    static void labeledBreakStatement(int iter, List<Integer> list) {
        List<LabeledBreakStatementFrame> stack = new ArrayList<>();
        stack.add(new LabeledBreakStatementFrame(iter, list));
        while (true) {
            LabeledBreakStatementFrame frame = stack.get(stack.size() - 1);
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
                    frame.block = true ? 6 : 5;
                    break;
                }
                case 5: {
                    stack.add(new LabeledBreakStatementFrame(frame.iter - 1, frame.list));
                    frame.block = 11;
                    break;
                }
                case 6: {
                    frame.block = true ? 7 : 3;
                    break;
                }
                case 7: {
                    frame.list.add(frame.iter);
                    frame.count--;
                    frame.block = frame.count == 0 ? 5 : 6;
                    break;
                }
                case 11: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class LabeledBreakStatementFrame {
        private int iter;
        private List<Integer> list;
        private int count;
        private int block;

        private LabeledBreakStatementFrame(int iter, List<Integer> list) {
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
