/******************************************************************************
 *  Compilation:  javac TowersOfHanoi.java
 *  Execution:    java TowersOfHanoi n
 *
 *  Solves the Towers of Hanoi problem on n discs. The discs are labeled
 *  in increasing order of size from 1 to n.
 *
 *  %  java TowersOfHanoi 3
 *  1 left
 *  2 right
 *  1 left
 *  3 left
 *  1 left
 *  2 right
 *  1 left
 *
 *  % java TowersOfHanoi 4
 *  1 right
 *  2 left
 *  1 right
 *  3 right
 *  1 right
 *  2 left
 *  1 right
 *  4 left
 *  1 right
 *  2 left
 *  1 right
 *  3 right
 *  1 right
 *  2 left
 *  1 right
 *
 ******************************************************************************/

public class TowersOfHanoi {

    // print out instructions for moving n discs to
    // the left (if left is true) or right (if left is false)
    public static void moves(int n, boolean left) {
        if (n == 0) return;
        moves(n - 1, !left);
        if (left) System.out.println(n + " left");
        else System.out.println(n + " right");
        moves(n - 1, !left);
    }

    public static void main(String[] args) {
        moves(4, true);
    }

}
