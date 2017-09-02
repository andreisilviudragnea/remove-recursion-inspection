import com.sun.istack.internal.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

class Dependency2 {

    public static boolean intersect(@NotNull Set<String> ids1, @NotNull Set<String> ids2) {
        final Deque<IntersectFrame> stack = new ArrayDeque<>();
        stack.push(new IntersectFrame(ids1, ids2));
        boolean ret = false;
        while (!stack.isEmpty()) {
            final IntersectFrame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    if (frame.ids1.size() > frame.ids2.size()) {
                        stack.push(new IntersectFrame(frame.ids2, frame.ids1));
                        frame.block = 3;
                        break;
                    } else {
                        for (String id : frame.ids1) {
                            if (frame.ids2.contains(id)) {
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
                    boolean temp = ret;
                    ret = temp;
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
        private int block;

        private IntersectFrame(Set<String> ids1, Set<String> ids2) {
            this.ids1 = ids1;
            this.ids2 = ids2;
        }
    }
}