import java.util.ArrayList;
import java.util.List;

class Dependency3 {
    int calculate(int one, int two, int three) {
        List<CalculateFrame> stack = new ArrayList<>();
        stack.add(new CalculateFrame(one, two, three));
        int ret = 0;
        while (true) {
            CalculateFrame frame = stack.get(stack.size() - 1);
            switch (frame.block) {
                case 0: {
                    stack.add(new CalculateFrame(frame.three + frame.two, frame.two + frame.one, frame.one));
                    frame.block = 1;
                    break;
                }
                case 1: {
                    frame.temp = ret;
                    ret = frame.temp;
                    if (stack.size() == 1)
                        return ret;
                    stack.remove(stack.size() - 1);
                    break;
                }
            }
        }
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