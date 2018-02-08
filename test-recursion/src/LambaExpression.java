import java.util.function.IntSupplier;

public class LambaExpression {
    static int factorial(int n) {
        final IntSupplier supplier = () -> {
            if (n == 1)
                return 1;
            return n * factorial(n - 1);
        };
        return supplier.getAsInt();
    }

    public static void main(String[] args) {
        System.out.println(factorial(12));
    }
}
