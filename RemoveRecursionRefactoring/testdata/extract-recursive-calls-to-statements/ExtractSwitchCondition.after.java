public class ExtractSwitchCondition {
    int recursive(int n) {
        if (n == 0)
            return 0;

        int temp = recursive(n - 1);
        int temp1 = recursive(temp);
        switch (temp1) {
            default:
        }

        return 3;
    }
}
