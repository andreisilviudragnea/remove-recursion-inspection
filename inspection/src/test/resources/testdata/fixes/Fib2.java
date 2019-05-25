public class Fib2 {
    static int fib2(int n) {
        if (n == 0) return 0;
        else if (n == 1) return 1;
        else return <caret>fib2(n - 1) + fib2(n - 2);
    }

    public static void main(String[] args) {
        System.out.println(Fib2.fib2(25));
    }
}
