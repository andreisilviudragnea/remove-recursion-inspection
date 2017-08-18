import org.jetbrains.annotations.Contract;

public class Primes {
  @Contract(pure = true)
  private static boolean isPrime(long x) {
    long limit = (long) Math.sqrt(x);
    for (long i = 2; i <= limit; i++) {
      if (x % i == 0) {
        return false;
      }
    }
    return true;
  }

  static long sumOfPrimes(long a) {
    if (a == 1)
      return 0;
    if (isPrime(a)) {
      return a + sumOfPrimes(a - 1);
    } else {
      return sumOfPrimes(a - 1);
    }
  }

  public static void main(String[] args) {
    System.out.println(sumOfPrimes(2_000_000));
  }
}
