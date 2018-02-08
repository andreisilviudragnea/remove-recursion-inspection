import java.util.ArrayList;
import java.util.List;

public class DoWhileStatement {
    static void doWhileStatement(int iter, List<Integer> list) {
        if (iter == 0)
            return;
        int count = iter;
        do {
            list.add(iter);
            count--;
        } while (count > 1);
        doWhileStatement(iter - 1, list);
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        DoWhileStatement.doWhileStatement(4, list);
        System.out.println(list);
    }
}
