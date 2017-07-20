/**
 * Proiectarea Algoritmilor, 2013
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 */

#ifndef NODE_H
#define NODE_H

#include "utils.h"

class Node {

	uint id;

    /**
     * Valoarea default pentru indexul unui nod nevizitat.
     */
    static const int UNSET = -1;

public:

	Node(uint id);
	Node(uint id, int value);

    const uint get_id() const;

    /**
     * Reprezinta cate noduri au fost vizitate inainte de nodul curent.
     */
    int index;

	/**
	 * Intoarce 'true' daca nodul a fost vizitat (index != UNSET).
	 */
	bool was_visited() const;

	/**
	 * Cel mai mic index al unui nod accesibil din nodul curent.
	 */
	int lowlink;

	/**
	 * (Optional) Descrie daca nodul curent se afla sau nu pe stiva,
	 * pentru a verifica acest aspect eficient in timpul algoritmului
	 * lui Tarjan.
	 */
	bool in_stack;

	/**
	 * Reseteaza valoarea atributelor index si lowlink
	 */
	void reset();

    /**
     * (Bonus) Valoarea unui nod.
     */
	int value;

	/** Pretty IO */
	friend std::istream& operator>>(std::istream& in, Node& node);
	friend std::ostream& operator<<(std::ostream& out, Node& node);
	friend std::ostream& operator<<(std::ostream& out, Node* node);
};

#endif
