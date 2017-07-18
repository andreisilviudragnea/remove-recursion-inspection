class Gcd {
    static int gcd(int m, int n) {
        if (m == n)
            return m;
        else if (m > n)
            return gcd(m - n, n);
        else
            return gcd(m, n - m);
    }

    static int gcd1(int m, int n) {
        if (m == n)
            return m;
        else if (m > n)
            return gcd1(m - n, n);
        return gcd1(m, n - m);
    }

    static int gcd2(int m, int n) {
        if (m == n)
            return m;
        if (m > n)
            return gcd2(m - n, n);
        return gcd2(m, n - m);
    }

    static int gcdTailRemoved(int m, int n) {
        while (true) {
            if (m == n)
                return m;
            else if (m > n) {
                m = m - n;
            } else {
                n = n - m;
            }
        }
    }

    static int gcd1TailRemoved(int m, int n) {
        while (true) {
            if (m == n)
                return m;
            else if (m > n) {
                m = m - n;
                continue;
            }
            n = n - m;
        }
    }

    static int gcd2TailRemoved(int m, int n) {
        while (true) {
            if (m == n)
                return m;
            if (m > n) {
                m = m - n;
                continue;
            }
            n = n - m;
        }
    }

    public static void main(String[] args) {
        System.out.println(gcd(468, 24));
        System.out.println(gcdTailRemoved(468, 24));
    }
}
