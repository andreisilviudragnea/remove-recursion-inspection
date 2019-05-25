public class ExtractNoChange2 {
    static int fib(int n) {
        if (n == 0) {
            return 0;
        } else {
            if (n == 1) {
                return 1;
            } else {
                int f1 = fib(n - 1), f2 = fib(n - 2);
                return f1 + f2;
            }
        }
    }
}
