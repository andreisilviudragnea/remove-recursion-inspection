/**
 * Proiectarea Algoritmilor, 2013
 * Lab 7: Aplicatii DFS
 * 
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 */

#include "node.h"

Node::Node(uint id) : id(id), value(-1) {
    reset();
}

Node::Node(uint id, int value) : id(id), value(value) {
    reset();
}

bool Node::was_visited() const {
    return index != Node::UNSET;
}

const uint Node::get_id() const {
	return id;
}

void Node::reset() {
    index   = Node::UNSET;
    lowlink = Node::UNSET;
}

std::ostream& operator<<(std::ostream& out, Node& node) {
	out << "(id: " << node.id << ")";
	return out;
}

std::ostream& operator<<(std::ostream& out, Node* node) {
	out << *node;
	return out;
}
