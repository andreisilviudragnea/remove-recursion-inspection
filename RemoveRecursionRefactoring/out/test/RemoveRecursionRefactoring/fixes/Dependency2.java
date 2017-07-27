import com.sun.istack.internal.NotNull;

import java.util.Set;

class Dependency2 {

    public static boolean intersect(@NotNull Set<String> ids1, @NotNull Set<String> ids2) {
        if (ids1.size() > ids2.size()) return <caret>intersect(ids2, ids1);
        for (String id : ids1) {
            if (ids2.contains(id)) return true;
        }
        return false;
    }
}