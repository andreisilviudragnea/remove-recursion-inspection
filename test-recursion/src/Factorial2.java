class Factorial2 {
    static int factorial2(int n) {
        if (n == 0)
            return 1;
        return n * factorial2(n - 1);
    }

    public static void main(String[] args) {
        System.out.println(Factorial2.factorial2(12));
    }
}
