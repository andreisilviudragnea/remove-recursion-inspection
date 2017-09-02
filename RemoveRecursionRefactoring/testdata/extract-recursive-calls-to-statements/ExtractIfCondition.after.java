public class ExtractIfCondition {
    int recursive(int n) {
        if (n == 0) {
            return 0;
        }

        int temp = recursive(n);
        int temp1 = recursive(n - 1);
        int temp2 = recursive(temp1);
        if (temp == temp2) {
            return 3;
        }

        return 3;
    }
}
