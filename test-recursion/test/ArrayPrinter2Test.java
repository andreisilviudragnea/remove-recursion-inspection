import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ArrayPrinter2Test {
    private void displayArray(Integer[] array) {
        final ArrayPrinter2 arrayPrinter = new ArrayPrinter2(array);
        final List<Integer> result = new ArrayList<>();

        arrayPrinter.displayArray(0, array.length - 1, result);
        assertEquals(Arrays.asList(array), result);
    }

    @Test
    public void displayArrayEven() throws Exception {
        displayArray(new Integer[]{1, 2, 3, 4});
    }

    @Test
    public void displayArrayOdd() throws Exception {
        displayArray(new Integer[]{1, 2, 3, 4, 5});
    }
}