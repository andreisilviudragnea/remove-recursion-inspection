public class ExtractForCondition {
    int <caret>recursive(int n) {
        for (int i = 0; i < recursive(3 + recursive(n)); i++) {
            System.out.println(i);
        }
        return 0;
    }
}
