import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class BreakStatement1 {
    static void breakStatement(int iter, List<Integer> list) {
        final Deque<BreakStatementFrame> stack = new ArrayDeque<>();
        stack.push(new BreakStatementFrame(iter, list));
        while (!stack.isEmpty()) {
            final BreakStatementFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.iter == 0) {
                        stack.pop();
                        break;
                    }
                    frame.count = frame.iter;
                    frame.block = 1;
                    break;
                }
                case 1: {
                    frame.list.add(frame.iter);
                    frame.count--;
                    stack.push(new BreakStatementFrame(frame.iter - 1, frame.list));
                    frame.block = 3;
                    break;
                }
                case 2: {
                    frame.list.add(frame.iter);
                    stack.pop();
                    break;
                }
                case 3: {
                    if (frame.count == 0) {
                        frame.block = 2;
                        break;
                    }
                    frame.block = 1;
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
        BreakStatement1.breakStatement(4, list);
        System.out.println(list);
    }
}
