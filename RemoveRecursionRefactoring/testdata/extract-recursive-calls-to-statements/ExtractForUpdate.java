public class ExtractForUpdate {
    int <caret>recursive(int n) {
        for (int i = 0; i < 3; i = recursive(n + recursive(n - 1))) {
            System.out.println(i);
        }
        return 0;
    }
}
