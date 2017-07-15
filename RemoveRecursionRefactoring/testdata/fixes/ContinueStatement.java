import java.util.ArrayList;
import java.util.List;

public class ContinueStatement {
    static void cont(int iter, List<Integer> list) {
        if (iter == 0)
            return;
        int count = iter;
        while (true) {
            count--;
            if (count > 2)
                continue;
            list.add(iter);
            if (count == 0)
                break;
        }
        <caret>cont(iter - 1, list);
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        ContinueStatement.cont(4, list);
        System.out.println(list);
    }
}
