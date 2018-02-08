public class Fib1 {
    static int fib1(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib1(n - 1) + fib1(n - 2);
    }

    public static void main(String[] args) {
        System.out.println(Fib1.fib1(25));
    }
}
