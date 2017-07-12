public class Factorial1 {
    static int factorial1(int n) {
        if (n == 0)
            return 1;
        else
            return n * factorial1(n - 1);
    }

    public static void main(String[] args) {
        System.out.println(Factorial1.factorial1(12));
    }
}
