import java.util.List;

class ArrayPrinter1 {
    private Integer[] array;

    ArrayPrinter1(Integer[] array) {
        this.array = array;
    }

    void displayArray(int first, int last, List<Integer> result) {
        if (first == last)
            result.add(array[first]);
        else {
            int mid;
            mid = first + (last - first) / 2;
            <caret>displayArray(first, mid, result);
            displayArray(mid + 1, last, result);
        }
    }
}
