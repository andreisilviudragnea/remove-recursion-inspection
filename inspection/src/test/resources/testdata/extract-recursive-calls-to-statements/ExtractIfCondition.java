public class ExtractIfCondition {
    int <caret>recursive(int n) {
        if (n == 0)
            return 0;

        if (recursive(n) == recursive(recursive(n - 1))) {
            return 3;
        }

        return 3;
    }
}
