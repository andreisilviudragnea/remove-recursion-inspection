import org.junit.Test;

import static org.junit.Assert.*;

public class FactorialTailTransformTest {
    @Test
    public void factorial() throws Exception {
        assertEquals(479001600, FactorialTailTransform.factorial(12, 1));
    }

    @Test
    public void factorialTailRemoved() throws Exception {
        assertEquals(479001600, FactorialTailTransform.factorialTailRemoved(12, 1));
    }

}