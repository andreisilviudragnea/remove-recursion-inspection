import com.sun.istack.internal.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class Dependency2 {

    public static boolean intersect(@NotNull Set<String> ids1, @NotNull Set<String> ids2) {
        List<IntersectFrame> stack = new ArrayList<>();
        stack.add(new IntersectFrame(ids1, ids2));
        boolean ret = false;
        while (true) {
            IntersectFrame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    frame.block = frame.ids1.size() > frame.ids2.size() ? 1 : 2;
                    break;
                }
                case 1: {
                    stack.add(new IntersectFrame(frame.ids2, frame.ids1));
                    frame.block = 3;
                    break;
                }
                case 2: {
                    frame.iterator = frame.ids1.iterator();
                    frame.block = 4;
                    break;
                }
                case 3: {
                    frame.temp = ret;
                    ret = frame.temp;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 4: {
                    frame.block = frame.iterator.hasNext() ? 5 : 6;
                    break;
                }
                case 5: {
                    frame.id = frame.iterator.next();
                    frame.block = frame.ids2.contains(frame.id) ? 7 : 4;
                    break;
                }
                case 6: {
                    ret = false;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
                case 7: {
                    ret = true;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
    }

    private static class IntersectFrame {
        private Set<String> ids1;
        private Set<String> ids2;
        private boolean temp;
        private Iterator<String> iterator;
        private String id;
        private int block;

        private IntersectFrame(Set<String> ids1, Set<String> ids2) {
            this.ids1 = ids1;
            this.ids2 = ids2;
        }
    }
}