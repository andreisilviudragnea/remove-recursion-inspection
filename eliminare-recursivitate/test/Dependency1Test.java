import org.junit.Test;

import static org.junit.Assert.*;

public class Dependency1Test {

    @Test
    public void factorial() throws Exception {
        assertEquals(3628800, new Dependency1().factorial(10));
    }
}