import java.util.Deque;

public class ArrayPrinter2 {
    private Object[] array;

    ArrayPrinter2(Object[] array) {
        this.array = array;
    }

    void displayArray2(int first, int last) {
        Deque<DisplayArray2Frame> stack = new java.util.ArrayDeque<>();
        stack.push(new DisplayArray2Frame(first, last));
        while (!stack.isEmpty()) {
            DisplayArray2Frame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.first == frame.last) {
                        System.out.print(array[frame.first] + " ");
                        stack.pop();
                        break switchLabel;
                    }
                    frame.mid = frame.first + (frame.last - frame.first) / 2;
                    frame.mid1 = frame.first + (frame.last - frame.first) / 2;
                    stack.push(new DisplayArray2Frame(frame.first, frame.mid));
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    stack.push(new DisplayArray2Frame(frame.mid1 + 1, frame.last));
                    frame.block = 2;
                    break switchLabel;
                }
                case 2: {
                    stack.pop();
                    break switchLabel;
                }
            }
        }
    }

    private static class DisplayArray2Frame {
        private int first;
        private int last;
        private int mid;
        private int mid1;
        private int block;

        private DisplayArray2Frame(int first, int last) {
            this.first = first;
            this.last = last;
        }
    }

    public static void main(String[] args) {
        Object[] array = {1, 2, 3, 4};
        ArrayPrinter2 arrayPrinter = new ArrayPrinter2(array);
        arrayPrinter.displayArray2(0, array.length - 1);
    }
}
