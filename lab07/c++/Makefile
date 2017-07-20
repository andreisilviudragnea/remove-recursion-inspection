CC = g++
CFLAGS = -g -Wall -std=c++11
SRC = $(wildcard *.cpp)
OBJ = $(SOURCES:.cpp=.o)
BIN = p1 p2 bonus
DEPS = utils.h

.PHONY: all clean

all: build

build: build-p1 build-p2 build-bonus

build-p1: 
	$(CC) p1.cpp node.cpp graph.cpp -o p1 $(CFLAGS)

build-p2: 
	$(CC) p2.cpp node.cpp graph.cpp -o p2 $(CFLAGS)

build-bonus:
	$(CC) bonus.cpp node.cpp graph.cpp -o bonus $(CFLAGS)

run-p1: p1 
	./p1

run-p2: p2
	./p2

run-bonus: bonus
	./bonus
	
clean:
	rm -f $(OBJ) $(BIN) *~ *.out *.o
