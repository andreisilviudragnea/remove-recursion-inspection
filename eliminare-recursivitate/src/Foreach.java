import java.util.Arrays;
import java.util.List;

public class Foreach {
  public static void main(String[] args) {
    final List<Integer> list = Arrays.asList(0, 1, 2);
    for (Integer integer : list) {
      System.out.println(integer);
    }
    final int[] array = {0, 1, 2};
    for (int n : array) {
      System.out.println(n);
    }
  }
}
