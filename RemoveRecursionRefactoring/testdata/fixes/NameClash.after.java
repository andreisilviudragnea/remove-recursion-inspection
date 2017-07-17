import java.util.ArrayList;
import java.util.List;

public class NameClash {
    private Object[] array;

    NameClash(Object[] array) {
        this.array = array;
    }

    void nameClash(int frame, int stack) {
        List<NameClashFrame> stack1 = new ArrayList<>();
        stack1.add(new NameClashFrame(frame, stack));
        while (true) {
            NameClashFrame frame1 = stack1.get(stack1.size() - 1);
            switchLabel:
            switch (frame1.block) {
                case 0: {
                    if (frame1.frame == frame1.stack) {
                        System.out.print(array[frame1.frame] + " ");
                        if (stack1.size() == 1)
                            return;
                        stack1.remove(stack1.size() - 1);
                        break switchLabel;
                    }
                    frame1.ret = frame1.frame + (frame1.stack - frame1.frame) / 2;
                    frame1.temp = frame1.frame + (frame1.stack - frame1.frame) / 2;
                    stack1.add(new NameClashFrame(frame1.frame, frame1.ret));
                    frame1.block = 1;
                    break switchLabel;
                }
                case 1: {
                    stack1.add(new NameClashFrame(frame1.temp + 1, frame1.stack));
                    frame1.block = 2;
                    break switchLabel;
                }
                case 2: {
                    if (stack1.size() == 1)
                        return;
                    stack1.remove(stack1.size() - 1);
                    break switchLabel;
                }
            }
        }
    }

    private static class NameClashFrame {
        private int frame;
        private int stack;
        private int ret;
        private int temp;
        private int block;

        private NameClashFrame(int frame, int stack) {
            this.frame = frame;
            this.stack = stack;
        }
    }

    public static void main(String[] args) {
        Object[] array = {1, 2, 3, 4};
        NameClash nameClash = new NameClash(array);
        nameClash.nameClash(0, array.length - 1);
    }
}
