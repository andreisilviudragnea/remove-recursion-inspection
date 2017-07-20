/**
 * Proiectarea Algoritmilor, 2016
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 * @author adinu
 * @email  mandrei.dinu@gmail.com
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class P2 {

    static void DfsArticulationPoints(P1.Graph g, P1.Node node, int father) {
        // TODO
    }

    /**
     * Calculeaza si afiseaza punctele de articulatie din graf </br>
     * 
     * Useful API:                                            </br> 
     * List with the articulation points of the graph.        </br>
     * graph.articulationPoints                               </br>
     * 
     * Complexitate solutie: O(N + M) </br>
     * N - numarul de noduri          </br>
     * M - numarul de muchii          </br>
     * @param g
     */
    static void ArticulationPoints(P1.Graph g) {
        g.reset();

        // TODO
        // HINT: use dfsArticulationPoints after you implement it
      
       System.out.println("\nPuncte. de articulatie: \n" + g.articulationPoints);       
    }

    // ********************************************************************** //
        
    static void DfsCriticalEdges(P1.Graph g, P1.Node node, int father) {
        // TODO
    }

    /**
     * Calculeaza si afiseaza muchiile critice din graf. </br>
     * 
     * Useful API:
     * Lista cu muchiile critice      </br>
     * graph.criticalEdges            </br>
     *
     * Complexitate solutie: O(N + M) </br>
     * N - numarul de noduri          </br>
     * M - numarul de muchii          </br>
     * 
     * @param g
     */
    static void CriticalEdges(P1.Graph g) {
        g.reset();

        // TODO
        // Hint: use dfsCriticalEdges after you implement it

        System.out.println("\nMuchii critice: \n" + g.criticalEdges);
    }

    final static String PATH = "./res/test02";

    public static void main(String... args) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(PATH));

        int test_count = scanner.nextInt();
        for (int i = 1; i <= test_count; ++i) {
            System.out.println("TEST " + i + "\n");

            P1.Graph g = new P1.Graph(P1.Graph.GraphType.UNDIRECTED);
            g.readData(scanner);

            System.out.println(g);
            ArticulationPoints(g);
            CriticalEdges(g);
        }

        scanner.close();
    }
}
