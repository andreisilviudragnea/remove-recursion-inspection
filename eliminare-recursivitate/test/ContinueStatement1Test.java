import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ContinueStatement1Test {
  @Test
  public void cont() throws Exception {
    final List<Integer> list = new ArrayList<>();
    ContinueStatement1.cont(4, list);
    assertEquals(Arrays.asList(4, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 4, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 4, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 4, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 2, 1, 1, 2, 1, 1, 2, 3, 4), list);
  }

}