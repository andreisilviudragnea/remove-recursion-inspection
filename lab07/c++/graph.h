/**
 * Proiectarea Algoritmilor, 2013
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 */

#ifndef GRAPH_H
#define GRAPH_H

#include "node.h"
#include "utils.h"

enum GraphType {
	DIRECTED, UNDIRECTED
};

class Graph {

	/** Tipul grafului: orientat/neorientat **/
	GraphType graph_type;

	/** Nodurile din graf **/
	std::vector<Node*> nodes;

	/** Lista de adiacenta **/
	std::vector<std::vector<Node*> > edges;

public:

	Graph();
	Graph(GraphType type);
	virtual ~Graph();

	/** Numarul de noduri din graf. **/
	uint node_count() const;

	/** Adauga nodul primit ca argument in graf. **/
	void insert_node(Node* node);

	/** Construieste o muchie intre cele doua noduri primite ca argument. **/
	void insert_edge(Node* node1, Node* node2);

	/** Intoarce un vector cu toate nodurile din graf. **/
	std::vector<Node*>& get_nodes();

	/** Intoarce un vector cu toti vecinii unui nod. **/
	std::vector<Node*>& get_neighbours(const Node& node);

	/** Removes all temporary information related to the state of the graph
	 *  during traversals (for example it sets every node as being unvisited)
	 */
	void reset();

	int time;

	/** Part I: Structuri auxiliare pentru determinarea componentelor tare conexe **/
	std::stack<Node*> stack;
	std::vector<std::vector<Node*> > ctc;

	void print_ctc() const;

	/** Part II: Structuri auxiliare pentru muchii & noduri critice. **/
	std::stack<std::pair<int, int> > edges_stack;
	std::vector<Node*> articulation_points;
	std::vector<std::pair<Node*, Node*> > critical_edges;

	/** Convenience overload of the IO operators for pretty printing/reading a graph */
	friend std::istream& operator>>(std::istream& in, Graph& node);
	friend std::ostream& operator<<(std::ostream& out, Graph& node);
};

#endif
