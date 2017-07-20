/*
 * bonus.cpp
 * Proiectarea Algoritmilor, 2016
 * Lab 7: Aplicatii DFS
 *
 * @author  Radu Iacob
 * @email   radu.iacob23@gmail.com
 */

#include "graph.h"
#include "node.h"
#include "utils.h"

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

int bonus (Graph& input_graph) {
    return 0;
}

int main () {

    std::fstream fin("bonus.in", std::fstream::in);

    Graph g(DIRECTED);
    fin >> g;
    std::cout << g << "\n";

    auto nodes = g.get_nodes();
    for (uint i = 0; i < g.node_count(); ++i) {
        fin >> nodes[i]->value;
    }

    std::cout << ((bonus(g) == 27) ? "Correct!\n" : "Wrong value, expected 27!\n");
    return 0;
}

