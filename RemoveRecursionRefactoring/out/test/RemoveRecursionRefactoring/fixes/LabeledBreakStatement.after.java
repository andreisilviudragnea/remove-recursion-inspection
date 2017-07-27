import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class LabeledBreakStatement {
    static void labeledBreakStatement(int iter, List<Integer> list) {
        Deque<LabeledBreakStatementFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new LabeledBreakStatementFrame(iter, list));
        while (!stack.isEmpty()) {
            LabeledBreakStatementFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.iter == 0) {
                        stack.pop();
                        break;
                    }
                    frame.count = frame.iter;
                    here:
                    while (true) {
                        while (true) {
                            frame.list.add(frame.iter);
                            frame.count--;
                            if (frame.count == 0) {
                                break here;
                            }
                        }
                    }
                    stack.push(new LabeledBreakStatementFrame(frame.iter - 1, frame.list));
                    frame.block = 1;
                    break;
                }
                case 1: {
                    stack.pop();
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
