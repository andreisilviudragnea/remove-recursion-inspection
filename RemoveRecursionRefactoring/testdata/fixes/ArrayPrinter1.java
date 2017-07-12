public class ArrayPrinter1 {
    private Object[] array;

    ArrayPrinter1(Object[] array) {
        this.array = array;
    }

    void displayArray1(int first, int last) {
        if (first == last)
            System.out.print(array[first] + " ");
        else {
            int mid;
            mid = first + (last - first) / 2;
            <caret>displayArray1(first, mid);
            displayArray1(mid + 1, last);
        }
    }

    public static void main(String[] args) {
        Object[] array = {1, 2, 3, 4, 5};
        ArrayPrinter1 arrayPrinter = new ArrayPrinter1(array);
        arrayPrinter.displayArray1(0, array.length - 1);
    }
}
