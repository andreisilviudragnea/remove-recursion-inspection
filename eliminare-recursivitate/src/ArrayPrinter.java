class ArrayPrinter {
    private Object[] array;

    ArrayPrinter(Object[] array) {
        this.array = array;
    }

    void displayArray1(int first, int last) {
        if (first == last)
            System.out.print(array[first] + " ");
        else {
            int mid;
            mid = first + (last - first) / 2;
            displayArray1(first, mid);
            displayArray1(mid + 1, last);
//            for (int i = 0; i < 3; i++)
//                ;
//            if (true)
//                System.out.println();
//            else if (true)
//                System.out.println(true);
//            else
//                System.out.println(false);
//            do
//                do
//                    System.out.println(true);
//                while (false);
//            while (false);
//            for (int i = 0, k = 0; i < 3 && k < 3; i++, k++)
//                for (int j = 0; j < 3; j++)
//                    System.out.println(true);
//            while (true)
//                while (true)
//                    System.out.println(true);
//            int[] a = new int[]{1, 2, 3};
//            for (int x : a)
//                for (int y : a)
//                    System.out.println(x + y);
        }
    }

    void displayArray2(int first, int last) {
        if (first == last) {
            System.out.print(array[first] + " ");
            return;
        }
        int mid = first + (last - first) / 2, mid1 = first + (last - first) / 2;
        displayArray2(first, mid);
        displayArray2(mid1 + 1, last);
    }

    static int fib(int n) {
        if (n == 0) return 0;
        else if (n == 1) return 1;
        else return fib(n - 1) + fib(n - 2);
    }

    static int fib1(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib1(n - 1) + fib1(n - 2);
    }
}
