public class ArrayPrinter2 {
    private Object[] array;

    ArrayPrinter2(Object[] array) {
        this.array = array;
    }

    void displayArray2(int first, int last) {
        if (first == last) {
            System.out.print(array[first] + " ");
            return;
        }
        int mid = first + (last - first) / 2, mid1 = first + (last - first) / 2;
        displayArray2(first, mid);
        displayArray2(mid1 + 1, last);
    }

    public static void main(String[] args) {
        Object[] array = {1, 2, 3, 4};
        ArrayPrinter2 arrayPrinter = new ArrayPrinter2(array);
        arrayPrinter.displayArray2(0, array.length - 1);
    }
}
