import org.junit.Test;

import static org.junit.Assert.*;

public class FibTailTransformTest {
    @Test
    public void fib1() throws Exception {
        assertEquals(75025, FibTailTransform.fib1(25, 0));
    }

    @Test
    public void fib1TailRemoved() throws Exception {
        assertEquals(75025, FibTailTransform.fib1TailRemoved(25, 0));
    }

    @Test
    public void fib2() throws Exception {
        assertEquals(75025, FibTailTransform.fib2(25, 0));
    }

    @Test
    public void fib2TailRemoved() throws Exception {
        assertEquals(75025, FibTailTransform.fib2TailRemoved(25, 0));
    }
}