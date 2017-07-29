import com.sun.istack.internal.NotNull;

import java.util.Deque;
import java.util.Iterator;
import java.util.Set;

class Dependency2 {

    public static boolean intersect(@NotNull Set<String> ids1, @NotNull Set<String> ids2) {
        Deque<IntersectFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new IntersectFrame(ids1, ids2));
        boolean ret = false;
        while (!stack.isEmpty()) {
            IntersectFrame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.ids1.size() > frame.ids2.size()) {
                        stack.push(new IntersectFrame(frame.ids2, frame.ids1));
                        frame.block = 3;
                        break;
                    } else {
                        for (frame.iterator = frame.ids1.iterator(); frame.iterator.hasNext(); ) {
                            frame.id = frame.iterator.next();
                            if (frame.ids2.contains(frame.id)) {
                                ret = true;
                                stack.pop();
                                break switchLabel;
                            }
                        }
                        ret = false;
                        stack.pop();
                        break;
                    }
                }
                case 3: {
                    frame.temp = ret;
                    ret = frame.temp;
                    stack.pop();
                    break;
                }
            }
        }
        return ret;
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