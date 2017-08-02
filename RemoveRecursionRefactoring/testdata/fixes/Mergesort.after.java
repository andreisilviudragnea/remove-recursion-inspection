import java.util.ArrayDeque;
import java.util.Deque;

public class Mergesort {
    private int[] numbers;
    private int[] helper;

    private int number;

    public void sort(int[] values) {
        this.numbers = values;
        number = values.length;
        this.helper = new int[number];
        mergesort(0, number - 1);
    }

    private void mergesort(int low, int high) {
        Deque<MergesortFrame> stack = new ArrayDeque<>();
        stack.push(new MergesortFrame(low, high));
        while (!stack.isEmpty()) {
            MergesortFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    if (frame.low < frame.high) {
                        frame.middle = frame.low + (frame.high - frame.low) / 2;
                        stack.push(new MergesortFrame(frame.low, frame.middle));
                        frame.block = 3;
                        break;
                    } else {
                        frame.block = 2;
                        break;
                    }
                }
                case 2: {
                    stack.pop();
                    break;
                }
                case 3: {
                    stack.push(new MergesortFrame(frame.middle + 1, frame.high));
                    frame.block = 4;
                    break;
                }
                case 4: {
                    for (int i = frame.low; i <= frame.high; i++) {
                        helper[i] = numbers[i];
                    }
                    int i = frame.low;
                    int j = frame.middle + 1;
                    int k = frame.low;
                    while (i <= frame.middle && j <= frame.high) {
                        if (helper[i] <= helper[j]) {
                            numbers[k] = helper[i];
                            i++;
                        } else {
                            numbers[k] = helper[j];
                            j++;
                        }
                        k++;
                    }
                    while (i <= frame.middle) {
                        numbers[k] = helper[i];
                        k++;
                        i++;
                    }
                    frame.block = 2;
                    break;
                }
            }
        }
    }

    private static class MergesortFrame {
        private int low;
        private int high;
        private int middle;
        private int block;

        private MergesortFrame(int low, int high) {
            this.low = low;
            this.high = high;
        }
    }

}