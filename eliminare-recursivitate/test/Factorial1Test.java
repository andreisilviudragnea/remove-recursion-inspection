import org.junit.Test;

import static org.junit.Assert.*;

public class Factorial1Test {
    @Test
    public void factorial1() throws Exception {
        assertEquals(479001600, Factorial1.factorial1(12));
    }

}