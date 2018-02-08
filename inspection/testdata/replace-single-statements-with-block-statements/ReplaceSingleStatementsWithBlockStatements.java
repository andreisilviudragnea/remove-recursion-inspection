class ReplaceSingleStatementsWithBlockStatements {
    void <caret>test() {
        for (int i = 0; i < 3; i++)
            ;
        if (true)
            System.out.println();
        else if (true)
            System.out.println(true);
        else
            System.out.println(false);
        if (true)
            if (true)
                System.out.println(true);
            else
                System.out.println(false);
        do
            do
                System.out.println(true);
            while (false);
        while (false);
        for (int i = 0, k = 0; i < 3 && k < 3; i++, k++)
            for (int j = 0; j < 3; j++)
                System.out.println(true);
        while (false)
            while (true)
                System.out.println(true);
        int[] a = new int[]{1, 2, 3};
        for (int x : a)
            for (int y : a)
                System.out.println(x + y);
        int x = 7;
        switch (x) {
            case 0:
                System.out.println();
                switch (x) {
                    case 1:
                        System.out.println();
                    default:
                        System.out.println();
                }
            case 1:
                System.out.println();
                break;
            case 2: {
                System.out.println();
            }
            case 3: {
                System.out.println();
                break;
            }
            case 7:
                ;
                break;
            case 6:
                ;
            case 5:
            case 4: {
                System.out.println();
            }
            break;
            default:
                System.out.println();
        }
    }
}
