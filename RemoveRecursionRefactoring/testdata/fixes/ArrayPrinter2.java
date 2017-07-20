import java.util.List;

class ArrayPrinter2 {
    private Integer[] array;

    ArrayPrinter2(Integer[] array) {
        this.array = array;
    }

    void displayArray(int first, int last, List<Integer> result) {
        if (first == last) {
            result.add(array[first]);
            return;
        }
        int mid = first + (last - first) / 2, mid1 = first + (last - first) / 2;
        <caret>displayArray(first, mid, result);
        displayArray(mid1 + 1, last, result);
    }
}
