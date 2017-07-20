/**
 * Proiectarea Algoritmilor, 2016
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 * @author adinu
 * @email  mandrei.dinu@gmail.com
 */

package graph;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;

public class Graph {

	public enum GraphType {
		DIRECTED, UNDIRECTED
	};

	public GraphType graphType;

	ArrayList<Node> nodes;
	ArrayList<ArrayList<Node>> edges;

	public int time;
	
	/**
	 * Partea I - Structuri auxiliare pentru determinarea componentelor tare conexe
	 */
	public Stack<Node> stack;
	public ArrayList<ArrayList<Node>> ctc;

	/**
	 * Partea II - Structuri auxiliare pentru muchii & noduri critice.
	 */
	public ArrayList<Node> articulationPoints;
	public ArrayList<Pair<Node, Node>> criticalEdges;

	public Graph(GraphType type) {
		nodes = new ArrayList<Node>();
		edges = new ArrayList<ArrayList<Node>>();

		stack = new Stack<Node>();
		ctc = new ArrayList<ArrayList<Node>>();

		articulationPoints = new ArrayList<Node>();
		criticalEdges = new ArrayList<Pair<Node, Node>>();

		graphType = type;
	}

	/**
	 * @return Numarul de noduri din graf.
	 */
	public int getNodeCount() {
		return nodes.size();
	}

	/**
	 * Construieste o muchie intre cele doua noduri primite ca argument.
	 * @param node1
	 * @param node2
	 */
	public void insertEdge(Node node1, Node node2) {
		edges.get(node1.getId()).add(node2);
	}

	/**
	 * Adauga nodul primit ca argument in graf.
	 * @param node
	 */
	public void insertNode(Node node) {
		nodes.add(node);
		edges.add(new ArrayList<Node>());
	}

	/**
	 * @return Intoarce o lista cu toate nodurile din graf.
	 */
	public ArrayList<Node> getNodes() {
		return nodes;
	}

	/**
	 * @param node
	 * @return Intoarce o lista cu toti vecinii unui nod.
	 */
	public ArrayList<Node> getNeighbours(Node node) {
		return edges.get(node.getId());
	}

	/**
	 * Reseteaza informatiile din structurile auxiliare din graf.
	 */
	public void reset() {

	    /**
		 * Reseteaza starea fiecarui nod 
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
	public void printCTC() {
		System.out.println("Componentele tare conexe:");
		for (ArrayList<Node> c_ctc : ctc) {
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

	public void readData(Scanner scanner) {

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
			if (graphType == Graph.GraphType.UNDIRECTED) {
				insertEdge(nodes.get(node2), nodes.get(node1));
			}
		}

	}

	@Override
	public String toString() {
		StringBuilder ans = new StringBuilder();

		ans.append("Graph:\n");
		for (Node n : nodes) {
			ans.append(n.toString() + " : ");
			ans.append(edges.get(n.getId()));
			ans.append('\n');
		}
		ans.append('\n');
		return ans.toString();
	}
}
