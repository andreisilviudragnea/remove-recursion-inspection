import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class BreakStatementTest {
    @Test
    public void breakStatement() throws Exception {
        final List<Integer> list = new ArrayList<>();
        BreakStatement.breakStatement(4, list);
        assertEquals(Arrays.asList(4, 4, 4, 4, 3, 3, 3, 2, 2, 1), list);
    }

}