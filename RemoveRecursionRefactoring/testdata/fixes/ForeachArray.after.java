import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ForeachArray {
    static void recursive(int n, List<Integer> result) {
        Deque<RecursiveFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new RecursiveFrame(n, result));
        while (!stack.isEmpty()) {
            RecursiveFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        stack.pop();
                        break;
                    }
                    frame.numbers = new Integer[]{0};
                    frame.i = 0;
                    frame.block = 1;
                    break;
                }
                case 1: {
                    if (frame.i < frame.numbers.length) {
                        frame.number = frame.numbers[frame.i];
                        frame.result.add(frame.n);
                        stack.push(new RecursiveFrame(frame.n - 1, frame.result));
                        frame.block = 3;
                        break;
                    } else {
                        stack.pop();
                        break;
                    }
                }
                case 3: {
                    frame.i++;
                    frame.block = 1;
                    break;
                }
            }
        }
    }

    private static class RecursiveFrame {
        private int n;
        private List<Integer> result;
        private Integer[] numbers;
        private int i;
        private Integer number;
        private int block;

        private RecursiveFrame(int n, List<Integer> result) {
            this.n = n;
            this.result = result;
        }
    }

    public static void main(String[] args) {
        final List<Integer> result = new ArrayList<>();
        recursive(5, result);
        System.out.println(result);
    }
}
