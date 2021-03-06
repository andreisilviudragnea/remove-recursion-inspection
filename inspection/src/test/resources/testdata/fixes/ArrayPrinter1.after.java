import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class ArrayPrinter1 {
    private Integer[] array;

    ArrayPrinter1(Integer[] array) {
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
                        frame.block = 3;
                        break;
                    } else {
                        frame.mid = frame.first + (frame.last - frame.first) / 2;
                        stack.push(new DisplayArrayFrame(frame.first, frame.mid, frame.result));
                        frame.block = 4;
                        break;
                    }
                }
                case 3: {
                    stack.pop();
                    break;
                }
                case 4: {
                    stack.push(new DisplayArrayFrame(frame.mid + 1, frame.last, frame.result));
                    frame.block = 3;
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
