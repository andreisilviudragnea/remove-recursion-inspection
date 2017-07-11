// Java program to print DFS traversal from a given given graph

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// This class represents a directed graph using adjacency list
// representation
class Graph {
    private int V;   // No. of vertices

    // Array  of lists for Adjacency List Representation
    private List<List<Integer>> adj;

    // Constructor
    private Graph(int v) {
        V = v;
        adj = new ArrayList<>();
        for (int i = 0; i < v; ++i)
            adj.add(new ArrayList<>());
    }

    // Function to add an edge into the graph
    private void addEdge(int v, int w) {
        adj.get(v).add(w);  // Add w to v's list.
    }

    // A function used by DFS
    private void DFSUtil(int v, boolean visited[]) {
        // Mark the current node as visited and print it
        visited[v] = true;
        System.out.print(v + " ");

        // Recur for all the vertices adjacent to this vertex
        Iterator<Integer> i = adj.get(v).listIterator();
        while (i.hasNext()) {
            int n = i.next();
            if (!visited[n])
                DFSUtil(n, visited);
            for (int j = 0; j < 3; j++)
                System.out.println(4);
        }
    }

    // The function to do DFS traversal. It uses recursive DFSUtil()
    void DFS(int v) {
        // Mark all the vertices as not visited(set as
        // false by default in java)
        boolean visited[] = new boolean[V];

        // Call the recursive helper function to print DFS traversal
        DFSUtil(v, visited);
    }

    public static void main(String args[]) {
        Graph g = new Graph(4);

        g.addEdge(0, 1);
        g.addEdge(0, 2);
        g.addEdge(1, 2);
        g.addEdge(2, 0);
        g.addEdge(2, 3);
        g.addEdge(3, 3);

        System.out.println("Following is Depth First Traversal " +
                "(starting from vertex 2)");

        g.DFS(2);
    }
}
// This code is contributed by Aakash Hasija
