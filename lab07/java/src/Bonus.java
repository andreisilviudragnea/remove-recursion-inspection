import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

public class Bonus {

    final static String PATH = "./res/bonus";

    /*
    * TODO:
    * Task: Se da un graf orientat aciclic (DAG).
    * Fiecare nod are asociata o valoare numerica.
    * Care este ciclul de valoare maxima care se poate
    * obtine prin inserarea unei muchii auxiliare?
    *
    * Complexitate solutie: O(N + M)
    * N - numarul de noduri
    * M - numarul de muchii
    */

    public static int bonus(P1.Graph g) {
        return 0;
    }

    public static void main(String... args) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(PATH));

        P1.Graph g = new P1.Graph(P1.Graph.GraphType.UNDIRECTED);
        g.readData(scanner);

        List<P1.Node> nodes = g.getNodes();
        for (int i = 0; i < g.getNodeCount(); ++i) {
            P1.Node node = nodes.get(i);
            node.setValue(scanner.nextInt());
        }

        if (bonus(g) != 27) {
            System.out.println("Wrong value, expected 27");
        } else {
            System.out.println("Correct!");
        }

        scanner.close();
    }

}
