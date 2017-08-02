import java.util.*;

public class LocalVariableSameName {
    static void recursive(int n, List<Number> result) {
        Deque<RecursiveFrame> stack = new ArrayDeque<>();
        stack.push(new RecursiveFrame(n, result));
        while (!stack.isEmpty()) {
            RecursiveFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.n == 0) {
                        stack.pop();
                        break;
                    }
                    for (int i = 0; i < 2; i++) {
                        final Integer val = i;
                        frame.result.add(val);
                    }
                    for (int i = 0; i < 3; i++) {
                        Double val = (double) i;
                        frame.result.add(val);
                        for (int j = 0; j < 1; j++) {
                            final Integer val1 = j;
                            frame.result.add(val1);
                        }
                    }
                    for (Integer i : Arrays.asList(0, 1)) {
                        frame.result.add(i);
                    }
                    for (Integer i : Arrays.asList(0, 1)) {
                        frame.result.add(i);
                        for (Integer j : Arrays.asList(1, 2)) {
                            frame.result.add(j);
                        }
                    }
                    stack.push(new RecursiveFrame(frame.n - 1, frame.result));
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

    private static class RecursiveFrame {
        private int n;
        private List<Number> result;
        private int block;

        private RecursiveFrame(int n, List<Number> result) {
            this.n = n;
            this.result = result;
        }
    }

    public static void main(String[] args) {
        List<Number> result = new ArrayList<>();
        recursive(3, result);
        System.out.println(result);
    }
}
