/**
 * Proiectarea Algoritmilor, 2016
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 * @author adinu
 * @email  mandrei.dinu@gmail.com
 */

package lab7;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import graph.Graph;
import graph.Node;

public class P1 {

    /**
     * Algoritmul lui Tarjan pentru determinarea componentelor tare conexe </br>
     * Complexitate: O(N + M)                                              </br>
     * unde                                                                </br>
     * N - nr de noduri                                                    </br>
     * M - nr de muchii                                                    </br>
     * 
     * Useful API:                                                         </br>
     * 
     * Lista cu vecinii nodului                                            </br> 
     * graph.getEdges(Node)                                                </br>
     * 
     * Stiva cu nodurile din componenta tare conexa curenta.               </br>
     * graph.stack                                                         </br>
     * 
     * Variabila booleana - true daca nodul se afla pe stiva               </br>
     * node.inStack                                                        </br>
     * 
     * Reprezinta cate noduri au fost vizitate inainte de nodul curent.    </br>
     * node.index                                                          </br>
     * 
     * In functie de timpul de descoperire, intoarce true daca             </br>
     * nodul a mai fost vizitat.                                           </br>
     * node.wasVisited()                                                   </br>
     * 
     * Cel mai mic timp de descoperire                                     </br> 
     * al unui nod accesibil din nodul curent.                             </br>
     * node.lowLink                                                        </br>
     */

    static void dfsCTC(Graph g, Node node) {
        
    }

    /**
     * Identifica componentele tare conexe din graful primit ca parametru.
     */
    static void StronglyConnectedComponents(Graph g) {
        
        g.reset(); // reseteaza starea variabilelor auxiliare din graf

        /** TODO: Apeleaza dfs_ctc pentru fiecare nod nevizitat. */

        g.printCTC(); // afiseaza componentele tare conexe
    }

    final static String PATH = "./res/test01";

    public static void main(String... args) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File(PATH));
        
        int test_count = scanner.nextInt();
        for (int i = 1; i <= test_count; ++i) {
            System.out.println("TEST " + i + "\n");
            Graph g = new Graph(Graph.GraphType.DIRECTED);
            g.readData(scanner);
            
            System.out.print(g);
            StronglyConnectedComponents(g);
            System.out.println("###########################");
        }

        scanner.close();
    }    
}
