import java.util.Deque;
import java.util.List;

class ArrayPrinter3 {
    private Integer[] array;

    ArrayPrinter3(Integer[] array) {
        this.array = array;
    }

    void displayArray(int first, int last, List<Integer> result) {
        Deque<DisplayArrayFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new DisplayArrayFrame(first, last, result));
        while (!stack.isEmpty()) {
            DisplayArrayFrame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    frame.block = true ? 2 : 3;
                    break switchLabel;
                }
                case 2: {
                    frame.block = frame.first == frame.last ? 4 : 6;
                    break switchLabel;
                }
                case 3: {
                    stack.pop();
                    break switchLabel;
                }
                case 4: {
                    frame.result.add(array[frame.first]);
                    stack.pop();
                    break switchLabel;
                }
                case 6: {
                    frame.mid = frame.first + (frame.last - frame.first) / 2;
                    stack.push(new DisplayArrayFrame(frame.first, frame.mid, frame.result));
                    frame.block = 7;
                    break switchLabel;
                }
                case 7: {
                    frame.first = frame.mid + 1;
                    frame.block = 1;
                    break switchLabel;
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
