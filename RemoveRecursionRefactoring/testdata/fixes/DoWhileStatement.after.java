import java.util.ArrayList;
import java.util.List;

public class DoWhileStatement {
    static void doWhileStatement(int iter, List<Integer> list) {
        List<DoWhileStatementFrame> stack = new ArrayList<>();
        stack.add(new DoWhileStatementFrame(iter, list));
        while (true) {
            DoWhileStatementFrame frame = stack.get(stack.size() - 1);
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
                    frame.block = 4;
                    break;
                }
                case 4: {
                    frame.list.add(frame.iter);
                    frame.count--;
                    frame.block = 3;
                    break;
                }
                case 3: {
                    frame.block = frame.count > 1 ? 4 : 5;
                    break;
                }
                case 5: {
                    stack.add(new DoWhileStatementFrame(frame.iter - 1, frame.list));
                    frame.block = 6;
                    break;
                }
                case 6: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class DoWhileStatementFrame {
        int iter;
        List<Integer> list;
        int block;
        int count;

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
