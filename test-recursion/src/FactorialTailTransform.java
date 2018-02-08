class FactorialTailTransform {
    static int factorial(int n, int acc) {
        if (n == 0)
            return acc * 1;
        return factorial(n - 1, acc * n);
    }

    static int factorialTailRemoved(int n, int acc) {
        while (true) {
            if (n == 0)
                return acc * 1;
            acc = acc * n;
            n = n - 1;
        }
    }
}
