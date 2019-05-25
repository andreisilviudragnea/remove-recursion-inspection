public class FibSwitch {
    static int <caret>fib(int n) {
        int ret;
        switch (n) {
            case 0:
                ret = 0;
                break;
            case 1:
                ret = 1;
                break;
            default: {
                ret = fib(n - 1) + fib(n - 2);
            }
        }
        return ret;
    }

    public static void main(String[] args) {
        System.out.println(FibSwitch.fib(25));
    }
}
