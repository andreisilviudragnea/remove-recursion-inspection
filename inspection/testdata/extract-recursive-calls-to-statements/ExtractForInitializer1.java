public class ExtractForInitializer1 {
    int <caret>recursive(int n) {
        for (int i = recursive(n - 1); i < 3; i++) {
            System.out.println(i);
        }
        return 0;
    }
}
