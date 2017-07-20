/**
 * Proiectarea Algoritmilor, 2016
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 * @author adinu
 * @email  mandrei.dinu@gmail.com
 */

#include "graph.h"


/**
 * Calculeaza si afiseaza punctele de articulatie din graf
 *
 * Useful API:
 * graph.articulation_points
 *
 * Complexitate solutie: O(N + M)
 * N - numarul de noduri
 * M - numarul de muchii
 */

void dfs_articulation_points(Graph& g, Node* node, int father) {
	// TODO
}

void compute_articulation_points(Graph& g) {
	g.reset(); // reseteaza variabilele auxiliare

	// TODO

	std::cout << "Punctele de articulatie: \n";
	std::cout << g.articulation_points << "\n";
}

// **************************************************************//


/**
 * Calculeaza si afiseaza muchiile critice din graf
 *
 * Useful API:
 * graph.critical_edges
 *
 * Complexitate solutie: O(N + M)
 * N - numarul de noduri
 * M - numarul de muchii
 */

void dfs_critical_edges(Graph& g, Node* node, int father) {
	// TODO
}

void compute_critical_edges(Graph& g) {
	g.reset(); // reseteaza variabilele auxiliare

	// TODO
    std::cout << "Muchiile critice: \n";
	std::cout << g.critical_edges << "\n";
}

int main(int argc, char* argv[]) {

    std::fstream fin("test_undirected.in", std::fstream::in);

	int nr_teste;
	fin >> nr_teste;

	for (int i = 1; i <= nr_teste; ++i) {

	    std::cout << "TEST " << i << "\n";
		Graph g2(UNDIRECTED);
		fin >> g2;
		std::cout << g2;

		compute_articulation_points(g2);
		compute_critical_edges(g2);
		std::cout << "################################\n";
	}

	return 0;
}
