public class ExtractNested {
    static int <caret>fib(int n, int acc) {
        if (n == 0) {
            return acc + 0;
        }
        else {
            if (n == 1) {
                return acc + 1;
            } else {
                return fib(n - 2, acc + fib(n - 1, 0));
            }
        }
    }
}
