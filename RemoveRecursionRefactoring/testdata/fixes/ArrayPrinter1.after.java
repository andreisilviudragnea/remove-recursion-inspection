import java.util.Deque;

public class ArrayPrinter1 {
    private Object[] array;

    ArrayPrinter1(Object[] array) {
        this.array = array;
    }

    void displayArray1(int first, int last) {
        Deque<DisplayArray1Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new DisplayArray1Frame(first, last));
        while (!stack.isEmpty()) {
            DisplayArray1Frame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    frame.block = frame.first == frame.last ? 1 : 3;
                    break switchLabel;
                }
                case 1: {
                    System.out.print(array[frame.first] + " ");
                    frame.block = 2;
                    break switchLabel;
                }
                case 3: {
                    frame.mid = frame.first + (frame.last - frame.first) / 2;
                    stack.push(new DisplayArray1Frame(frame.first, frame.mid));
                    frame.block = 4;
                    break switchLabel;
                }
                case 2: {
                    stack.pop();
                    break switchLabel;
                }
                case 4: {
                    stack.push(new DisplayArray1Frame(frame.mid + 1, frame.last));
                    frame.block = 2;
                    break switchLabel;
                }
            }
        }
    }

    private static class DisplayArray1Frame {
        private int first;
        private int last;
        private int mid;
        private int block;

        private DisplayArray1Frame(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    public static void main(String[] args) {
        Object[] array = {1, 2, 3, 4, 5};
        ArrayPrinter1 arrayPrinter = new ArrayPrinter1(array);
        arrayPrinter.displayArray1(0, array.length - 1);
    }
}
