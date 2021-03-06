import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class ArrayPrinter3 {
    private Integer[] array;

    ArrayPrinter3(Integer[] array) {
        this.array = array;
    }

    void displayArray(int first, int last, List<Integer> result) {
        final Deque<DisplayArrayFrame> stack = new ArrayDeque<>();
        stack.push(new DisplayArrayFrame(first, last, result));
        while (!stack.isEmpty()) {
            final DisplayArrayFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    frame.block = 1;
                    break;
                }
                case 1: {
                    if (frame.first == frame.last) {
                        frame.result.add(array[frame.first]);
                        stack.pop();
                        break;
                    } else {
                        frame.mid = frame.first + (frame.last - frame.first) / 2;
                        stack.push(new DisplayArrayFrame(frame.first, frame.mid, frame.result));
                        frame.block = 6;
                        break;
                    }
                }
                case 6: {
                    frame.first = frame.mid + 1;
                    frame.block = 1;
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
        private int block;

        private DisplayArrayFrame(int first, int last, List<Integer> result) {
            this.first = first;
            this.last = last;
            this.result = result;
        }
    }
}
