import org.junit.Test;

import static org.junit.Assert.*;

public class PrimesTest {
  @Test
  public void sumOfPrimes() throws Exception {
    assertEquals(142913828922L, Primes.sumOfPrimes(2_000_000));
  }

}