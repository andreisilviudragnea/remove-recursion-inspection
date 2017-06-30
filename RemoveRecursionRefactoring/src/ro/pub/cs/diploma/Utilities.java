package ro.pub.cs.diploma;

import org.jetbrains.annotations.NotNull;

class Utilities {
    @NotNull
    static String capitalize(String input) {
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
