import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class DoWhileStatementTest {
    @Test
    public void doWhileStatement() throws Exception {
        final List<Integer> list = new ArrayList<>();
        DoWhileStatement.doWhileStatement(4, list);
        assertEquals(Arrays.asList(4, 4, 4, 3, 3, 2, 1), list);
    }

}