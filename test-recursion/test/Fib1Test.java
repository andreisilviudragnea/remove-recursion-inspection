import org.junit.Test;

import static org.junit.Assert.*;

public class Fib1Test {
    @Test
    public void fib1() throws Exception {
        assertEquals(75025, Fib1.fib1(25));
    }

}