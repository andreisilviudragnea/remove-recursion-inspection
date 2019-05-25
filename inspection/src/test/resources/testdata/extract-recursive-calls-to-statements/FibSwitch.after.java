public class FibSwitch {
    static int fib(int n) {
        int ret;
        switch (n) {
            case 0:
                ret = 0;
                break;
            case 1:
                ret = 1;
                break;
            default: {
                int temp = fib(n - 1);
                int temp1 = fib(n - 2);
                ret = temp + temp1;
            }
        }
        return ret;
    }

    public static void main(String[] args) {
        System.out.println(FibSwitch.fib(25));
    }
}
