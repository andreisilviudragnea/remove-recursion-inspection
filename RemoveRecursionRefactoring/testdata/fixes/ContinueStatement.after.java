import java.util.ArrayList;
import java.util.List;

public class ContinueStatement {
    static void cont(int iter, List<Integer> list) {
        List<ContFrame> stack = new ArrayList<>();
        stack.add(new ContFrame(iter, list));
        while (true) {
            ContFrame frame = stack.get(stack.size() - 1);
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
                    stack.add(new ContFrame(frame.iter - 1, frame.list));
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
