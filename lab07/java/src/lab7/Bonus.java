package lab7;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import graph.Graph;
import graph.Node;

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
    
    public static int bonus(Graph g) {        
        return 0;
    }
    
    public static void main(String... args) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(PATH));

        Graph g = new Graph(Graph.GraphType.UNDIRECTED);
        g.readData(scanner);

        ArrayList<Node> nodes = g.getNodes();
        for (int i = 0; i < g.getNodeCount(); ++i) {
            Node node = nodes.get(i);            
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
