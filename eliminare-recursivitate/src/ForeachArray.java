import java.util.ArrayList;
import java.util.List;

public class ForeachArray {
    static void recursive(int n, List<Integer> result) {
        if (n == 0)
            return;

        Integer[] numbers = {0};
        for (Integer number : numbers) {
            result.add(n);
            recursive(n - 1, result);
        }

    }

    public static void main(String[] args) {
        final List<Integer> result = new ArrayList<>();
        recursive(5, result);
        System.out.println(result);
    }
}
