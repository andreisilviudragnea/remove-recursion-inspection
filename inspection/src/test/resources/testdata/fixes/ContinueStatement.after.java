import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ContinueStatement {
    static void cont(int iter, List<Integer> list) {
        final Deque<ContFrame> stack = new ArrayDeque<>();
        stack.push(new ContFrame(iter, list));
        while (!stack.isEmpty()) {
            final ContFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.iter == 0) {
                        stack.pop();
                        break;
                    }
                    frame.count = frame.iter;
                    while (true) {
                        frame.count--;
                        if (frame.count > 2) {
                            continue;
                        }
                        frame.list.add(frame.iter);
                        if (frame.count == 0) {
                            break;
                        }
                    }
                    stack.push(new ContFrame(frame.iter - 1, frame.list));
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

    private static class ContFrame {
        private int iter;
        private List<Integer> list;
        private int count;
        private int block;

        private ContFrame(int iter, List<Integer> list) {
            this.iter = iter;
            this.list = list;
        }
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        ContinueStatement.cont(4, list);
        System.out.println(list);
    }
}
