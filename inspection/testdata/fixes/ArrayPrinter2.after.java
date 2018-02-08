import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class ArrayPrinter2 {
    private Integer[] array;

    ArrayPrinter2(Integer[] array) {
        this.array = array;
    }

    void displayArray(int first, int last, List<Integer> result) {
        final Deque<DisplayArrayFrame> stack = new ArrayDeque<>();
        stack.push(new DisplayArrayFrame(first, last, result));
        while (!stack.isEmpty()) {
            final DisplayArrayFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.first == frame.last) {
                        frame.result.add(array[frame.first]);
                        stack.pop();
                        break;
                    }
                    frame.mid = frame.first + (frame.last - frame.first) / 2;
                    frame.mid1 = frame.first + (frame.last - frame.first) / 2;
                    stack.push(new DisplayArrayFrame(frame.first, frame.mid, frame.result));
                    frame.block = 1;
                    break;
                }
                case 1: {
                    stack.push(new DisplayArrayFrame(frame.mid1 + 1, frame.last, frame.result));
                    frame.block = 2;
                    break;
                }
                case 2: {
                    stack.pop();
                    break;
                }
            }
        }
    }

    private static class DisplayArrayFrame {
        private int first;
        private int last;
        private List<Integer> result;
        private int mid;
        private int mid1;
        private int block;

        private DisplayArrayFrame(int first, int last, List<Integer> result) {
            this.first = first;
            this.last = last;
            this.result = result;
        }
    }
}
