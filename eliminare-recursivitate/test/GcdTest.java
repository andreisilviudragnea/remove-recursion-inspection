import org.junit.Test;

import static org.junit.Assert.*;

public class GcdTest {
    @Test
    public void gcd() throws Exception {
        assertEquals(12, Gcd.gcd(468, 24));
    }

    @Test
    public void gcd1() throws Exception {
        assertEquals(12, Gcd.gcd1(468, 24));
    }

    @Test
    public void gcd2() throws Exception {
        assertEquals(12, Gcd.gcd2(468, 24));
    }

    @Test
    public void gcdTailRemoved() throws Exception {
        assertEquals(12, Gcd.gcdTailRemoved(468, 24));
    }

    @Test
    public void gcd1TailRemoved() throws Exception {
        assertEquals(12, Gcd.gcd1TailRemoved(468, 24));
    }

    @Test
    public void gcd2TailRemoved() throws Exception {
        assertEquals(12, Gcd.gcd2TailRemoved(468, 24));
    }
}