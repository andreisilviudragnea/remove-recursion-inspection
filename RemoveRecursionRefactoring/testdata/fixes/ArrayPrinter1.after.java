import java.util.ArrayList;
import java.util.List;

public class ArrayPrinter1 {
    private Object[] array;

    ArrayPrinter1(Object[] array) {
        this.array = array;
    }

    void displayArray1(int first, int last) {
        List<DisplayArray1Frame> stack = new ArrayList<>();
        stack.add(new DisplayArray1Frame(first, last));
        while (true) {
            DisplayArray1Frame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.first == frame.last ? 1 : 3;
                    break;
                }
                case 1: {
                    System.out.print(array[frame.first] + " ");
                    frame.block = 2;
                    break;
                }
                case 3: {
                    frame.mid = frame.first + (frame.last - frame.first) / 2;
                    stack.add(new DisplayArray1Frame(frame.first, frame.mid));
                    frame.block = 4;
                    break;
                }
                case 2: {
                    if (stack.size() == 1)
                        return;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 4: {
                    stack.add(new DisplayArray1Frame(frame.mid + 1, frame.last));
                    frame.block = 2;
                    break;
                }
            }
        }
    }

    private static class DisplayArray1Frame {
        int first;
        int last;
        int block;
        int mid;

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
