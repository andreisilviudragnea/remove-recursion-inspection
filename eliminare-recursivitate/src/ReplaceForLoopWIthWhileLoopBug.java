public class ReplaceForLoopWIthWhileLoopBug {
    private static int x;

    private static int inc() {
        return x++;
    }

    public static void main(String[] args) {
        for (int i = 0, j = 0; i < 3 && j < 3; i = inc(), j = inc()) {
            System.out.println(i + " " + j);
        }
    }
}
