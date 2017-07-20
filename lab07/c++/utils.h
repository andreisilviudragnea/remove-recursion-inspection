#ifndef UTILS_H
#define UTILS_H

#include<cstdio>
#include<cstdlib>
#include<cstring>

/*
 STL
 */

#include<map>
#include<set>
#include<stack>
#include<string>
#include<vector>
#include<bitset>
#include<algorithm>

#include<sstream>
#include<iostream>
#include<fstream>

#include <assert.h>

typedef unsigned int uint;

class InvalidQuery {

public:

	InvalidQuery(std::string error_msg) {
		std::cerr << "Invalid Query: " << error_msg << "\n";
	}

};

template<class T>
std::ostream& operator <<(std::ostream& os, const typename std::vector<T>& v) {
	for (typename std::vector<T>::const_iterator ii = v.begin(); ii != v.end();
			++ii) {
		os << " " << *ii;
	}
	os << "\n";

	return os;
}

template<class T>
std::ostream& operator <<(std::ostream& os,
		const typename std::vector<std::pair<T, T> >& v) {
	for (typename std::vector<std::pair<T, T> >::const_iterator ii = v.begin();
			ii != v.end(); ++ii) {
		os << "(" << *ii->first << " , " << *ii->second << ") ";
	}
	os << "\n";
	return os;
}

#endif
