import java.util.ArrayList;
import java.util.List;

public class LabeledBreakStatement {
    static void labeledBreakStatement(int iter, List<Integer> list) {
        if (iter == 0)
            return;
        int count = iter;
        here:
        while (true) {
            while (true) {
                list.add(iter);
                count--;
                if (count == 0)
                    break here;
            }
        }
        <caret>labeledBreakStatement(iter - 1, list);
    }

    public static void main(String[] args) {
        final ArrayList<Integer> list = new ArrayList<>();
        LabeledBreakStatement.labeledBreakStatement(4, list);
        System.out.println(list);
    }
}
