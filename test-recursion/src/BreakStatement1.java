import java.util.ArrayList;
import java.util.List;

public class BreakStatement1 {
    static void breakStatement(int iter, List<Integer> list) {
        if (iter == 0)
            return;
        int count = iter;
        while (true) {
            list.add(iter);
            count--;
            breakStatement(iter - 1, list);
            if (count == 0)
                break;
        }
        list.add(iter);
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        BreakStatement1.breakStatement(4, list);
        System.out.println(list);
    }
}
