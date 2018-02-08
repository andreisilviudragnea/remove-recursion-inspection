public class ExtractWhileCondition {
    int recursive(int n) {
        if (n == 0)
            return 0;

        while (recursive(recursive(n - 2) + 1) == recursive(n - 1)) {
            return 3;
        }

        return 3;
    }
}
