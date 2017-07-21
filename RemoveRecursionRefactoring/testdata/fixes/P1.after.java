/*
  Proiectarea Algoritmilor, 2016
  Lab 7: Aplicatii DFS

  @author Radu Iacob
 * @email radu.iacob23@gmail.com
 * @author adinu
 * @email mandrei.dinu@gmail.com
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class P1 {

    /**
     * Algoritmul lui Tarjan pentru determinarea componentelor tare conexe </br>
     * Complexitate: O(N + M)                                              </br>
     * unde                                                                </br>
     * N - nr de noduri                                                    </br>
     * M - nr de muchii                                                    </br>
     * <p>
     * Useful API:                                                         </br>
     * <p>
     * Lista cu vecinii nodului                                            </br>
     * graph.getEdges(Node)                                                </br>
     * <p>
     * Stiva cu nodurile din componenta tare conexa curenta.               </br>
     * graph.stack                                                         </br>
     * <p>
     * Variabila booleana - true daca nodul se afla pe stiva               </br>
     * node.inStack                                                        </br>
     * <p>
     * Reprezinta cate noduri au fost vizitate inainte de nodul curent.    </br>
     * node.index                                                          </br>
     * <p>
     * In functie de timpul de descoperire, intoarce true daca             </br>
     * nodul a mai fost vizitat.                                           </br>
     * node.wasVisited()                                                   </br>
     * <p>
     * Cel mai mic timp de descoperire                                     </br>
     * al unui nod accesibil din nodul curent.                             </br>
     * node.lowLink                                                        </br>
     */

    private static void dfsCTC(Graph g, Node node) {
        Deque<DfsCTCFrame> stack = new java.util.ArrayDeque<>();
        stack.push(new DfsCTCFrame(g, node));
        while (!stack.isEmpty()) {
            DfsCTCFrame frame = stack.peek();
            switch (frame.block) {
                case 0: {
                    frame.node.setIndex(frame.g.time);
                    frame.node.setLowLink(frame.g.time);
                    frame.g.time += 1;
                    frame.g.stack.push(frame.node);
                    frame.node.setInStack(true);
                    frame.iterator = frame.g.getNeighbours(frame.node).iterator();
                    frame.block = 1;
                    break;
                }
                case 1: {
                    frame.block = frame.iterator.hasNext() ? 2 : 3;
                    break;
                }
                case 2: {
                    frame.n = frame.iterator.next();
                    frame.block = !frame.n.wasVisited() ? 4 : 6;
                    break;
                }
                case 3: {
                    if (frame.node.getLowLink() == frame.node.getIndex()) {
                        frame.ctc = new ArrayList<>();
                        do {
                            frame.n = frame.g.stack.pop();
                            frame.n.setInStack(false);
                            frame.ctc.add(frame.n);
                        } while (!frame.n.equals(frame.node));
                        frame.g.ctc.add(frame.ctc);
                    }
                    stack.pop();
                    break;
                }
                case 4: {
                    stack.push(new DfsCTCFrame(frame.g, frame.n));
                    frame.block = 7;
                    break;
                }
                case 6: {
                    if (frame.n.isInStack()) {
                        frame.node.setLowLink(Math.min(frame.node.getLowLink(), frame.n.getIndex()));
                    }
                    frame.block = 1;
                    break;
                }
                case 7: {
                    frame.node.setLowLink(Math.min(frame.node.getLowLink(), frame.n.getLowLink()));
                    frame.block = 1;
                    break;
                }
            }
        }
    }

    private static class DfsCTCFrame {
        private Graph g;
        private Node node;
        private Iterator<Node> iterator;
        private Node n;
        private List<Node> ctc;
        private int block;

        private DfsCTCFrame(Graph g, Node node) {
            this.g = g;
            this.node = node;
        }
    }

    /**
     * Identifica componentele tare conexe din graful primit ca parametru.
     */
    static void StronglyConnectedComponents(Graph g) {

        g.reset(); // reseteaza starea variabilelor auxiliare din graf

        /* TODO: Apeleaza dfs_ctc pentru fiecare nod nevizitat. */
        for (Node node : g.getNodes()) {
            if (!node.wasVisited()) {
                dfsCTC(g, node);
            }
        }

        g.printCTC(); // afiseaza componentele tare conexe
    }

    private final static String PATH = "testdata/test01";

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

    public static class Graph {

        public enum GraphType {
            DIRECTED, UNDIRECTED
        }

        GraphType graphType;

        List<Node> nodes = new ArrayList<>();
        List<List<Node>> edges = new ArrayList<>();

        int time;

        /**
         * Partea I - Structuri auxiliare pentru determinarea componentelor tare conexe
         */
        Stack<Node> stack = new Stack<>();
        List<List<Node>> ctc = new ArrayList<>();

        /**
         * Partea II - Structuri auxiliare pentru muchii & noduri critice.
         */
        List<Node> articulationPoints = new ArrayList<>();
        List<Pair<Node, Node>> criticalEdges = new ArrayList<>();

        Graph(GraphType type) {
            graphType = type;
        }

        /**
         * @return Numarul de noduri din graf.
         */
        int getNodeCount() {
            return nodes.size();
        }

        /**
         * Construieste o muchie intre cele doua noduri primite ca argument.
         * @param node1
         * @param node2
         */
        void insertEdge(Node node1, Node node2) {
            edges.get(node1.getId()).add(node2);
        }

        /**
         * Adauga nodul primit ca argument in graf.
         * @param node
         */
        void insertNode(Node node) {
            nodes.add(node);
            edges.add(new ArrayList<>());
        }

        /**
         * @return Intoarce o lista cu toate nodurile din graf.
         */
        List<Node> getNodes() {
            return nodes;
        }

        /**
         * @param node
         * @return Intoarce o lista cu toti vecinii unui nod.
         */
        List<Node> getNeighbours(Node node) {
            return edges.get(node.getId());
        }

        /**
         * Reseteaza informatiile din structurile auxiliare din graf.
         */
        void reset() {

            /*
              Reseteaza starea fiecarui nod
             */
            for (Node n : nodes)
                n.reset();

            stack.clear();
            ctc.clear();

            articulationPoints.clear();
            criticalEdges.clear();

            time = 0;
        }

        /**
         * Afiseaza componentele tare conexe detectate in graf.
         */
        void printCTC() {
            System.out.println("Componentele tare conexe:");
            for (List<Node> c_ctc : ctc) {
                System.out.println(c_ctc);
            }
            System.out.println("\n");
        }

        /**
         *
         *
         * Format intrare: </br>
         * -- numar de noduri, numar de  muchii </br>
         * N M </br>
         * -- M linii de tipul (A, B) </br>
         * -- cu semnficatia ca exista muchie intre nodurile A si B </br>
         * Node1 Node2 </br>
         * ...
         *
         * @param scanner
         *            object, initialized with the input file </br>
         */

        void readData(Scanner scanner) {

            if (scanner == null)
                return;

            int numNodes = scanner.nextInt();
            int numEdges = scanner.nextInt();

            for (int i = 0; i < numNodes; ++i) {
                Node new_node = new Node(i);
                insertNode(new_node);
            }

            for (int i = 0; i < numEdges; ++i) {
                int node1 = scanner.nextInt();
                int node2 = scanner.nextInt();
                insertEdge(nodes.get(node1), nodes.get(node2));
                if (graphType == GraphType.UNDIRECTED) {
                    insertEdge(nodes.get(node2), nodes.get(node1));
                }
            }

        }

        @Override
        public String toString() {
            StringBuilder ans = new StringBuilder();

            ans.append("Graph:\n");
            for (Node n : nodes) {
                ans.append(n.toString()).append(" : ");
                ans.append(edges.get(n.getId()));
                ans.append('\n');
            }
            ans.append('\n');
            return ans.toString();
        }

        public List<List<Node>> getCtc() {
            return ctc;
        }
    }

    public static class Node {

        int id;

        Node(int id) {
            this.id = id;
            reset();
        }

        public Node(int id, int value) {
            this.id = id;
            this.value = value;
            reset();
        }

        /**
         * Reprezinta cate noduri au fost vizitate inainte de nodul curent. </br>
         */
        int index;

        /**
         * Valoarea default pentru indexul unui nod nevizitat. </br>
         */
        final static int UNSET = -1;

        /**
         * Cel mai mic index al unui nod accesibil din nodul curent. </br>
         */
        int lowLink;

        /**
         * (Optional) Descrie daca nodul curent se afla sau nu pe stiva,
         * pentru a verifica acest aspect eficient in timpul algoritmului
         * lui Tarjan.
         */
        boolean inStack;

        /**
         * (Bonus) Valoarea unui nod.
         */
        int value;

        int getIndex() {
            return index;
        }

        void setIndex(int index) {
            this.index = index;
        }

        int getLowLink() {
            return lowLink;
        }

        void setLowLink(int lowLink) {
            this.lowLink = lowLink;
        }

        boolean isInStack() {
            return inStack;
        }

        void setInStack(boolean inStack) {
            this.inStack = inStack;
        }

        public int getValue() {
            return value;
        }

        void setValue(int value) {
            this.value = value;
        }

        /**
         * @return 'true' daca nodul a fost vizitat, altfel intoarce false
         */
        boolean wasVisited() {
            return index != UNSET;
        }

        /**
         * Reseteaza valoarea atributelor index si lowLink
         */
        void reset() {
            index = lowLink = UNSET;
            inStack = false;
        }

        int getId() {
            return id;
        }

        @Override
        public String toString() {
            return "( " + Integer.toString(id) + " )";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            return id == node.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /**
     * Generic Pair class, scraped from stackoverflow
     */
    public static class Pair<A, B> {
        private A first;
        private B second;

        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int hashCode() {
            int hashFirst = first != null ? first.hashCode() : 0;
            int hashSecond = second != null ? second.hashCode() : 0;
            return (hashFirst + hashSecond) * hashSecond + hashFirst;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof Pair) {
                @SuppressWarnings("unchecked")
                Pair<A, B> otherPair = (Pair<A, B>) other;
                return ((this.first == otherPair.first
                        || (this.first != null && otherPair.first != null && this.first.equals(otherPair.first)))
                        && (this.second == otherPair.second || (this.second != null && otherPair.second != null
                        && this.second.equals(otherPair.second))));
            }

            return false;
        }

        @Override
        public String toString() {
            return "(" + first + ", " + second + ")";
        }

        public A getFirst() {
            return first;
        }

        public void setFirst(A first) {
            this.first = first;
        }

        public B getSecond() {
            return second;
        }

        public void setSecond(B second) {
            this.second = second;
        }
    }
}
