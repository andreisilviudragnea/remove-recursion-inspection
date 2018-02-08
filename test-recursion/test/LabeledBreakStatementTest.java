import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class LabeledBreakStatementTest {
    @Test
    public void labeledBreakStatement() throws Exception {
        final List<Integer> list = new ArrayList<>();
        LabeledBreakStatement.labeledBreakStatement(4, list);
        assertEquals(Arrays.asList(4, 4, 4, 4, 3, 3, 3, 2, 2, 1), list);
    }

}