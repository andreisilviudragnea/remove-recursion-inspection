import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LocalVariableSameNameTest {
    @Test
    public void recursive() throws Exception {
        List<Number> result = new ArrayList<>();
        LocalVariableSameName.recursive(3, result);
        System.out.println(result);
        assertEquals(Arrays.asList(0, 1, 0.0, 1.0, 2.0, 0, 1, 0.0, 1.0, 2.0, 0, 1, 0.0, 1.0, 2.0), result);
    }

}