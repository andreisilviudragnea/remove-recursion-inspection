import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class Dependency2Test {
    private boolean intersect(String[] ids1, String[] ids2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(ids1));
        Set<String> set2 = new HashSet<>(Arrays.asList(ids2));
        return Dependency2.intersect(set1, set2);
    }

    @Test
    public void intersectTrueGreater() throws Exception {
        assertTrue(intersect(new String[]{"a", "b", "c"}, new String[]{"b", "c"}));
    }

    @Test
    public void intersectTrueSmaller() throws Exception {
        assertTrue(intersect(new String[]{"b", "c"}, new String[]{"a", "b", "c"}));
    }

    @Test
    public void intersectFalseGreater() throws Exception {
        assertFalse(intersect(new String[]{"a", "b", "c"}, new String[]{"d", "e"}));
    }

    @Test
    public void intersectFalseSmaller() throws Exception {
        assertFalse(intersect(new String[]{"b", "c"}, new String[]{"a", "d", "e"}));
    }
}