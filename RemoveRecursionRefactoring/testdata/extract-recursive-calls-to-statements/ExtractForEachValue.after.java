import java.util.Arrays;

public class ExtractForEachValue {
    int recursive(int n) {
        int temp = recursive(n);
        int temp1 = recursive(n - 1 + temp);
        for (int i : Arrays.asList(temp1, 2)) {
            System.out.println(i);
        }
        return 0;
    }
}
