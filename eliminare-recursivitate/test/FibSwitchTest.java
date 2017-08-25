import org.junit.Test;

import static org.junit.Assert.*;

public class FibSwitchTest {
  @Test
  public void fib() throws Exception {
    assertEquals(75025, FibSwitch.fib(25));
  }
}