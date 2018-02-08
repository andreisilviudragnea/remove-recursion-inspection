public class NameClash {
    private Object[] array;

    NameClash(Object[] array) {
        this.array = array;
    }

    void nameClash(int frame, int stack) {
        if (frame == stack) {
            System.out.print(array[frame] + " ");
            return;
        }
        int ret = frame + (stack - frame) / 2, temp = frame + (stack - frame) / 2;
        nameClash(frame, ret);
        nameClash(temp + 1, stack);
    }

    public static void main(String[] args) {
        Object[] array = {1, 2, 3, 4};
        NameClash nameClash = new NameClash(array);
        nameClash.nameClash(0, array.length - 1);
    }
}
