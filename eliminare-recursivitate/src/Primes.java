public class Primes {
  private static final int NUM_ITERATIONS = 10;

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
    final long before = System.nanoTime();
    for (int i = 0; i < NUM_ITERATIONS; i++) {
      sumOfPrimes(10_000);
    }
    final long after = System.nanoTime();
    System.out.println("Elapsed time: " +
      (after - before) / 1_000 / NUM_ITERATIONS + "us");
  }
}
