import java.util.ArrayList;
import java.util.List;

public class BreakStatement {
  static void breakStatement(int iter, List<Integer> list) {
    if (iter == 0)
      return;
    int count = iter;
    while (true) {
      list.add(iter);
      count--;
      if (count == 0)
        break;
    }
    breakStatement(iter - 1, list);
  }

  public static void main(String[] args) {
    final ArrayList<Integer> list = new ArrayList<>();
    BreakStatement.breakStatement(4, list);
    System.out.println(list);
  }
}
