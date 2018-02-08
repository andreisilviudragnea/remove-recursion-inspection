import java.io.*;

public class FileSystem {

    public static void main(String[] args) {
        traverse();
    }

    /**
     * Obtains the filesystem roots
     * Proceeds with the recursive filesystem traversal
     */
    private static void traverse() {
        File[] fs = File.listRoots();
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].isDirectory() && fs[i].canRead()) {
                rtraverse(fs[i]);
            }
        }
    }

    /**
     * Recursively traverse a given directory
     *
     * @param fd indicates the starting point of traversal
     */
    private static void rtraverse(File fd) {
        // Section 1.
        File[] fss = fd.listFiles();

        for (int i = 0; i < fss.length; i++) {
            System.out.println(fss[i]);
            if (fss[i].isDirectory() && fss[i].canRead()) {
                // Section 2.
                rtraverse(fss[i]);
            }
        }
    }

}