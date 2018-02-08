public class ExtractNested {
    static int fib(int n, int acc) {
        if (n == 0) {
            return acc + 0;
        }
        else {
            if (n == 1) {
                return acc + 1;
            } else {
                int temp = fib(n - 1, 0);
                int temp1 = fib(n - 2, acc + temp);
                return temp1;
            }
        }
    }
}
