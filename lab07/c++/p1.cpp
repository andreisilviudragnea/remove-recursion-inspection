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
#include "utils.h"

/**
Algoritmul lui Tarjan pentru determinarea componentelor tare conexe
Complexitate: O( N + M )
unde
	N - nr de noduri
	M - nr de muchii

Useful API:

Vector cu vecinii nodului.
graph.get_edges(node)

Vector cu componentele tare conexe ale grafului.
graph.ctc

Stiva cu nodurile din componenta tare conexa curenta.
graph.stack

Variabila booleana - true daca nodul se afla pe stiva
node->in_stack

Cate noduri au fost vizitate inainte de nodul curent.
node->index

In functie de timpul de descoperire, intoarce true daca nodul a mai fost vizitat.
node->was_visited()

Cel mai mic index al unui nod accesibil din nodul curent.
node->lowlink
**/

void dfs_ctc(Graph& g, Node* node) {

}


/**
 * Identifica componentele tare conexe din graful primit ca parametru
 */
void StronglyConnectedComponents(Graph& g) {
    g.reset(); // reseteaza starea variabilelor auxiliare din graf

	/** TODO: Apeleaza dfs_ctc pentru fiecare nod nevizitat */
	g.print_ctc(); // afiseaza componentele tare conexe
	return;
}



int main(int argc, char* argv[]) {

    std::fstream fin("test_ctc.in", std::fstream::in);

	int nr_teste;
	fin >> nr_teste;

	assert(nr_teste > 0);

	for (int i = 1; i <= nr_teste; ++i) {

	    std::cout << "TEST " << i << "\n";

		Graph g1(DIRECTED);
		fin >> g1;
		std::cout << g1;

		StronglyConnectedComponents(g1);
        std::cout << "################################\n";
	}

	return 0;
}
