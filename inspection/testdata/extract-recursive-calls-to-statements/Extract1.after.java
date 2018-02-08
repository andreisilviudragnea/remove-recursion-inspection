public class Extract1 {
    static int fib(int n) {
        if (n == 0) {
            return 0;
        }
        else {
            if (n == 1) {
                return 1;
            } else {
                int temp = fib(n - 1);
                int temp1 = fib(n - 2);
                return temp + temp1;
            }
        }
    }
}
