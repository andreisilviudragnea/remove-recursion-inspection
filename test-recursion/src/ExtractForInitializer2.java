public class ExtractForInitializer2 {
    int recursive(int n) {
        int i;
        for (i = n + recursive(n - 1); i < 3; i++) {
            System.out.println(i);
        }
        return 0;
    }
}
