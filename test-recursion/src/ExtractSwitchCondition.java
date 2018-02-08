public class ExtractSwitchCondition {
    int recursive(int n) {
        if (n == 0)
            return 0;

        switch (recursive(recursive(n - 1))) {
            default:
        }

        return 3;
    }
}
