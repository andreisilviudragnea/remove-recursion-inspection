import java.util.ArrayList;
import java.util.List;

public class LabeledBreakStatement {
    static void labeledBreakStatement(int iter, List<Integer> list) {
        List<LabeledBreakStatementFrame> stack = new ArrayList<>();
        stack.add(new LabeledBreakStatementFrame(iter, list));
        while (true) {
            LabeledBreakStatementFrame frame = stack.get(stack.size() - 1);
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.iter == 0) {
                        if (stack.size() == 1)
                            return;
                        stack.remove(stack.size() - 1);
                        break switchLabel;
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
                    stack.add(new LabeledBreakStatementFrame(frame.iter - 1, frame.list));
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break switchLabel;
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
