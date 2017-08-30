import org.junit.Test;

import static org.junit.Assert.*;

public class FibSwitchFallThroughTest {
  @Test
  public void fib() throws Exception {
    assertEquals(121393, FibSwitchFallThrough.fib(25));
    assertEquals(46368, FibSwitchFallThrough.counter);
  }

}