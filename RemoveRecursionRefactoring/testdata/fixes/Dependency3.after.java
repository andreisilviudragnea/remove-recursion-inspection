import java.util.Deque;

class Dependency3 {
    int calculate(int one, int two, int three) {
        Deque<CalculateFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new CalculateFrame(one, two, three));
        int ret = 0;
        while (!stack.isEmpty()) {
            CalculateFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    stack.push(new CalculateFrame(frame.three + frame.two, frame.two + frame.one, frame.one));
                    frame.block = 1;
                    break;
                }
                case 1: {
                    int temp = ret;
                    ret = temp;
                    stack.pop();
                    break;
                }
            }
        }
        return ret;
    }

    private static class CalculateFrame {
        private int one;
        private int two;
        private int three;
        private int block;

        private CalculateFrame(int one, int two, int three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }
    }
}