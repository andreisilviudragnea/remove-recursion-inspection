import java.util.Arrays;

public class ExtractForEachValue {
    int <caret>recursive(int n) {
        for (int i : Arrays.asList(recursive(n - 1 + recursive(n)), 2)) {
            System.out.println(i);
        }
        return 0;
    }
}
