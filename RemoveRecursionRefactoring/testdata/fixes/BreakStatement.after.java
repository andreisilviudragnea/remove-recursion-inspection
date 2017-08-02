import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BreakStatement {
    static void breakStatement(int iter, List<Integer> list) {
        Deque<BreakStatementFrame> stack = new ArrayDeque<>();
        stack.push(new BreakStatementFrame(iter, list));
        while (!stack.isEmpty()) {
            BreakStatementFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.iter == 0) {
                        stack.pop();
                        break;
                    }
                    frame.count = frame.iter;
                    while (true) {
                        frame.list.add(frame.iter);
                        frame.count--;
                        if (frame.count == 0) {
                            break;
                        }
                    }
                    stack.push(new BreakStatementFrame(frame.iter - 1, frame.list));
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

    private static class BreakStatementFrame {
        private int iter;
        private List<Integer> list;
        private int count;
        private int block;

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
