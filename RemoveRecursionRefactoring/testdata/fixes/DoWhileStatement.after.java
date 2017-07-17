import java.util.ArrayList;
import java.util.List;

public class DoWhileStatement {
    static void doWhileStatement(int iter, List<Integer> list) {
        List<DoWhileStatementFrame> stack = new ArrayList<>();
        stack.add(new DoWhileStatementFrame(iter, list));
        while (true) {
            DoWhileStatementFrame frame = stack.get(stack.size() - 1);
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
                    do {
                        frame.list.add(frame.iter);
                        frame.count--;
                    } while (frame.count > 1);
                    stack.add(new DoWhileStatementFrame(frame.iter - 1, frame.list));
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

    private static class DoWhileStatementFrame {
        private int iter;
        private List<Integer> list;
        private int count;
        private int block;

        private DoWhileStatementFrame(int iter, List<Integer> list) {
            this.iter = iter;
            this.list = list;
        }
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        DoWhileStatement.doWhileStatement(4, list);
        System.out.println(list);
    }
}
