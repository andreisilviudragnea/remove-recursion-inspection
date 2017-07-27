import java.util.List;

class ArrayPrinter3 {
    private Integer[] array;

    ArrayPrinter3(Integer[] array) {
        this.array = array;
    }

    void displayArray(int first, int last, List<Integer> result) {
        while (true) {
            if (first == last) {
                result.add(array[first]);
                return;
            } else {
                int mid;
                mid = first + (last - first) / 2;
                <caret>displayArray(first, mid, result);
                first = mid + 1;
            }
        }
    }
}
