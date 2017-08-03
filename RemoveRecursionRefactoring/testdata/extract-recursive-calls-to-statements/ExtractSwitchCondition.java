public class ExtractSwitchCondition {
    int <caret>recursive(int n) {
        if (n == 0)
            return 0;

        switch (recursive(recursive(n - 1))) {
            default:
        }

        return 3;
    }
}
