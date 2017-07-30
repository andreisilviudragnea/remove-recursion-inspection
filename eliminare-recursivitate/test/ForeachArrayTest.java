import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ForeachArrayTest {
    @Test
    public void recursive() throws Exception {
        final List<Integer> result = new ArrayList<>();
        ForeachArray.recursive(5, result);
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

}