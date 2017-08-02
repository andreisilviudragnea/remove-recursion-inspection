import java.util.ArrayDeque;
import java.util.Deque;

public class NameClash {
    private Object[] array;

    NameClash(Object[] array) {
        this.array = array;
    }

    void nameClash(int frame, int stack) {
        Deque<NameClashFrame> stack1 = new ArrayDeque<>();
        stack1.push(new NameClashFrame(frame, stack));
        while (!stack1.isEmpty()) {
            NameClashFrame frame1 = stack1.peek();
            switch (frame1.block) {
                case 0: {
                    if (frame1.frame == frame1.stack) {
                        System.out.print(array[frame1.frame] + " ");
                        stack1.pop();
                        break;
                    }
                    frame1.ret = frame1.frame + (frame1.stack - frame1.frame) / 2;
                    frame1.temp = frame1.frame + (frame1.stack - frame1.frame) / 2;
                    stack1.push(new NameClashFrame(frame1.frame, frame1.ret));
                    frame1.block = 1;
                    break;
                }
                case 1: {
                    stack1.push(new NameClashFrame(frame1.temp + 1, frame1.stack));
                    frame1.block = 2;
                    break;
                }
                case 2: {
                    stack1.pop();
                    break;
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
