import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ContinueStatementTest {
    @Test
    public void cont() throws Exception {
        final List<Integer> list = new ArrayList<>();
        ContinueStatement.cont(5, list);
        assertEquals(Arrays.asList(5, 5, 5, 4, 4, 4, 3, 3, 3, 2, 2, 1), list);
    }

}