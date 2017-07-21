import java.util.*;

public class LocalVariableSameName {
    static void recursive(int n, List<Number> result) {
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
                    frame.i = 0;
                    while (frame.i < 2) {
                        frame.val = frame.i;
                        frame.result.add(frame.val);
                        frame.i++;
                    }
                    frame.i1 = 0;
                    while (frame.i1 < 3) {
                        frame.val2 = (double) frame.i1;
                        frame.result.add(frame.val2);
                        frame.j = 0;
                        while (frame.j < 1) {
                            frame.val1 = frame.j;
                            frame.result.add(frame.val1);
                            frame.j++;
                        }
                        frame.i1++;
                    }
                    frame.iterator = Arrays.asList(0, 1).iterator();
                    while (frame.iterator.hasNext()) {
                        frame.i2 = frame.iterator.next();
                        frame.result.add(frame.i2);
                    }
                    frame.iterator2 = Arrays.asList(0, 1).iterator();
                    while (frame.iterator2.hasNext()) {
                        frame.i3 = frame.iterator2.next();
                        frame.result.add(frame.i3);
                        frame.iterator1 = Arrays.asList(1, 2).iterator();
                        while (frame.iterator1.hasNext()) {
                            frame.j1 = frame.iterator1.next();
                            frame.result.add(frame.j1);
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
        private int i;
        private Integer val;
        private int i1;
        private Double val2;
        private int j;
        private Integer val1;
        private Iterator<Integer> iterator;
        private Integer i2;
        private Iterator<Integer> iterator2;
        private Integer i3;
        private Iterator<Integer> iterator1;
        private Integer j1;
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
