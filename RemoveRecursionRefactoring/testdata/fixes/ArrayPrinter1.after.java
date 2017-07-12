import java.util.ArrayList;
import java.util.List;

public class ArrayPrinter1 {
    private Object[] array;

    ArrayPrinter1(Object[] array) {
        this.array = array;
    }

    void displayArray1(int first, int last) {
        List<DisplayArray1Context> stack = new ArrayList<>();
        stack.add(new DisplayArray1Context(first, last));
        while (true) {
            DisplayArray1Context context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.first == context.last ? 1 : 3;
                    break;
                }
                case 1: {
                    System.out.print(array[context.first] + " ");
                    context.section = 2;
                    break;
                }
                case 2: {
                    if (stack.size() == 1)
                        return;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 3: {
                    context.mid = context.first + (context.last - context.first) / 2;
                    context.section = 4;
                    stack.add(new DisplayArray1Context(context.first, context.mid));
                    break;
                }
                case 4: {
                    context.section = 5;
                    stack.add(new DisplayArray1Context(context.mid + 1, context.last));
                    break;
                }
                case 5: {
                    context.section = 2;
                    break;
                }
            }
        }
    }

    private static class DisplayArray1Context {
        int first;
        int last;
        int section;
        int mid;

        private DisplayArray1Context(int first, int last) {
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
