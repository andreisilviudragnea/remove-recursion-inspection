public class ExtractForCondition {
    int recursive(int n) {
        int temp = recursive(n);
        int temp1 = recursive(3 + temp);
        for (int i = 0; i < temp1; i++) {
            System.out.println(i);
        }
        return 0;
    }
}
