import java.util.Deque;

class Dependency4 {
    int calculate(int one, int two, int three) {
        Deque<CalculateFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new CalculateFrame(one, two, three));
        int ret = 0;
        while (!stack.isEmpty()) {
            CalculateFrame frame = stack.peek();
            switchLabel:
            switch (frame.block) {
                case 0: {
                    stack.push(new CalculateFrame(frame.one ^ 2, frame.one * frame.two, frame.one + frame.two + frame.three));
                    frame.block = 1;
                    break switchLabel;
                }
                case 1: {
                    frame.temp = ret;
                    ret = frame.temp;
                    stack.pop();
                    break switchLabel;
                }
            }
        }
        return ret;
    }

    private static class CalculateFrame {
        private int one;
        private int two;
        private int three;
        private int temp;
        private int block;

        private CalculateFrame(int one, int two, int three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }
    }
}