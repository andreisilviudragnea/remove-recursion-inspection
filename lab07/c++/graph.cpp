/**
 * Proiectarea Algoritmilor, 2013
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 */

#include "graph.h"

Graph::Graph() :
		graph_type(DIRECTED) {
    reset();
}

Graph::Graph(GraphType type) :
		graph_type(type) {
    reset();
}

Graph::~Graph() {
    for_each(nodes.begin(), nodes.end(), [](Node* node) {delete node;});
}

uint Graph::node_count() const {
	return nodes.size();
}

void Graph::insert_node(Node* node) {
	nodes.push_back(node);
	edges.push_back(std::vector<Node*>());
}

void Graph::insert_edge(Node* node1, Node* node2) {
	edges[node1->get_id()].push_back(node2);
}

std::vector<Node*>& Graph::get_nodes() {
	return nodes;
}

std::vector<Node*>& Graph::get_neighbours(const Node& node) {
	return edges[node.get_id()];
}

void Graph::reset() {
    for_each(nodes.begin(), nodes.end(), [](Node* node){
        node->reset();
    });

	while (!stack.empty())
		stack.pop();
	ctc.clear();

	articulation_points.clear();
	critical_edges.clear();

	time = 0;
}


void Graph::print_ctc() const {
    std::cout << "Strongly Connected Components:\n";

	for(auto& comp : ctc) {
	    for(auto node : comp) {
	         std::cout << *node << ", ";
	    }
	    std::cout << "\n";
	}

	std::cout << "\n";
}

std::ostream& operator<<(std::ostream& out, Graph& graph) {

	out << "Print Graph :\n";

	for(uint i = 0; i < graph.nodes.size(); ++i) {
	    out << i << ": ";
	    for (uint j = 0; j < graph.edges[i].size(); ++j) {
	        if (j) {
	            out << ", ";
	        }
	        out << graph.edges[i][j]->get_id();
	    }
	    out << "\n";
	}

	return out;
}

/**
 Format intrare:
 -- numar de noduri, numar de  muchii
 N M
 -- M linii de tipul (A, B) cu semnficatia
 -- ca exista muchie intre nodurile A si B
 Node1 Node2
 ...
**/
std::istream& operator>>(std::istream& in, Graph& graph) {

    uint num_nodes, num_edges;
    in >> num_nodes >> num_edges;

    for (uint i = 0; i < num_nodes; ++i) {
        Node *node = new Node(i);
        graph.insert_node(node);
    }

    auto& nodes = graph.get_nodes();
    for (uint i = 0; i < num_edges; ++i) {

        uint node_idx_1, node_idx_2;
        in >> node_idx_1 >> node_idx_2;

        Node* node1 = nodes[node_idx_1];
        Node* node2 = nodes[node_idx_2];

        graph.insert_edge(node1, node2);
        if (graph.graph_type == UNDIRECTED) {
            graph.insert_edge(node2, node1);
        }
    }

	return in;
}
