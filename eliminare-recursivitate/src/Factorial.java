class Factorial {
    static int factorial1(int n) {
        if (n == 0)
            return 1;
        else
            return n * factorial1(n - 1);
    }

    static int factorial2(int n) {
        if (n == 0)
            return 1;
        return n * factorial2(n - 1);
    }
}
