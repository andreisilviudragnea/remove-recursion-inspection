import java.util.ArrayList;
import java.util.List;

public class ArrayPrinter2 {
    private Object[] array;

    ArrayPrinter2(Object[] array) {
        this.array = array;
    }

    void displayArray2(int first, int last) {
        List<DisplayArray2Context> stack = new ArrayList<>();
        stack.add(new DisplayArray2Context(first, last));
        while (true) {
            DisplayArray2Context context = stack.get(stack.size() - 1);
            switch (context.section) {
                case 0: {
                    context.section = context.first == context.last ? 1 : 2;
                    break;
                }
                case 1: {
                    System.out.print(array[context.first] + " ");
                    if (stack.size() == 1)
                        return;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
                case 2: {
                    context.mid = context.first + (context.last - context.first) / 2;
                    context.mid1 = context.first + (context.last - context.first) / 2;
                    context.section = 3;
                    stack.add(new DisplayArray2Context(context.first, context.mid));
                    break;
                }
                case 3: {
                    context.section = 4;
                    stack.add(new DisplayArray2Context(context.mid1 + 1, context.last));
                    break;
                }
                case 4: {
                    if (stack.size() == 1)
                        return;
                    else
                        stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class DisplayArray2Context {
        int first;
        int last;
        int section;
        int mid;
        int mid1;

        private DisplayArray2Context(int first, int last) {
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
