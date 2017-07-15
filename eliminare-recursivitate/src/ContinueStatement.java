import java.util.ArrayList;
import java.util.List;

public class ContinueStatement {
    static void cont(int iter, List<Integer> list) {
        List<ContFrame> stack = new ArrayList<>();
        stack.add(new ContFrame(iter, list));
        while (true) {
            ContFrame frame = stack.get(stack.size() - 1);
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
                    frame.block = 3;
                    break;
                }
                case 3: {
                    frame.block = true ? 4 : 5;
                    break;
                }
                case 4: {
                    frame.count--;
                    frame.block = frame.count > 2 ? 6 : 7;
                    break;
                }
                case 5: {
                    stack.add(new ContFrame(frame.iter - 1, frame.list));
                    frame.block = 10;
                    break;
                }
                case 6: {
                    frame.block = 3;
                    break;
                }
                case 7: {
                    frame.list.add(frame.iter);
                    frame.block = frame.count == 0 ? 8 : 9;
                    break;
                }
                case 8: {
                    frame.block = 5;
                    break;
                }
                case 9: {
                    frame.block = 3;
                    break;
                }
                case 10: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class ContFrame {
        int iter;
        List<Integer> list;
        int block;
        int count;

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
