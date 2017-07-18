class FibTailTransform {
    static int fib1(int n, int acc) {
        if (n == 0) return acc + 0;
        if (n == 1) return acc + 1;
        return fib1(n - 2, acc + fib1(n - 1, 0));
    }

    static int fib1TailRemoved(int n, int acc) {
        while (true) {
            if (n == 0) return acc + 0;
            if (n == 1) return acc + 1;
            acc = acc + fib1TailRemoved(n - 1, 0);
            n = n - 2;
        }
    }

    static int fib2(int n, int acc) {
        if (n == 0) return acc + 0;
        if (n == 1) return acc + 1;
        return fib2(n - 1, acc + fib2(n - 2, 0));
    }

    static int fib2TailRemoved(int n, int acc) {
        while (true) {
            if (n == 0) return acc + 0;
            if (n == 1) return acc + 1;
            acc = acc + fib2TailRemoved(n - 2, 0);
            n = n - 1;
        }
    }
}
