import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class DoWhileStatement {
    static void doWhileStatement(int iter, List<Integer> list) {
        Deque<DoWhileStatementFrame> stack = new ArrayDeque<>();
        stack.push(new DoWhileStatementFrame(iter, list));
        while (!stack.isEmpty()) {
            DoWhileStatementFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.iter == 0) {
                        stack.pop();
                        break;
                    }
                    frame.count = frame.iter;
                    do {
                        frame.list.add(frame.iter);
                        frame.count--;
                    } while (frame.count > 1);
                    stack.push(new DoWhileStatementFrame(frame.iter - 1, frame.list));
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
