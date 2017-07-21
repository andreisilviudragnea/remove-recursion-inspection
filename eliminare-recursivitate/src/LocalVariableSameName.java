import java.util.ArrayList;
import java.util.List;

public class LocalVariableSameName {
    static void recursive(int n, List<Number> result) {
        if (n == 0)
            return;

        for (int i = 0; i < 2; i++) {
            final Integer val = i;
            result.add(val);
        }

        for (int i = 0; i < 3; i++) {
            Double val = (double) i;
            result.add(val);
        }

        recursive(n - 1, result);
    }

    public static void main(String[] args) {
        List<Number> result = new ArrayList<>();
        recursive(3, result);
        System.out.println(result);
    }
}
