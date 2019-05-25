public class ExtractForInitializer2 {
    int recursive(int n) {
        int i;
        int temp = recursive(n - 1);
        for (i = n + temp; i < 3; i++) {
            System.out.println(i);
        }
        return 0;
    }
}
