import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ContinueStatement1 {
    static void cont(int iter, List<Integer> list) {
        Deque<ContFrame> stack = new ArrayDeque<>();
        stack.push(new ContFrame(iter, list));
        while (!stack.isEmpty()) {
            ContFrame frame = stack.peek();
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
                    frame.count--;
                    frame.list.add(frame.iter);
                    stack.push(new ContFrame(frame.iter - 1, frame.list));
                    frame.block = 3;
                    break;
                }
                case 2: {
                    frame.list.add(frame.iter);
                    stack.pop();
                    break;
                }
                case 3: {
                    if (frame.count > 2) {
                        frame.block = 1;
                        break;
                    }
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
        ContinueStatement1.cont(4, list);
        System.out.println(list);
    }
}
