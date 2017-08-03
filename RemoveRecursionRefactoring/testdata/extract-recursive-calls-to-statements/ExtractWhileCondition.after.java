public class ExtractWhileCondition {
    int recursive(int n) {
        if (n == 0)
            return 0;

        int temp = recursive(n - 2);
        int temp1 = recursive(temp + 1);
        int temp2 = recursive(n - 1);
        while (temp1 == temp2) {
            return 3;
        }

        return 3;
    }
}
