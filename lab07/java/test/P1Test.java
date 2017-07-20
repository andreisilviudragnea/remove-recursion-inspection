import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class P1Test {
    private final static String PATH = "./res/test01";

    private static final List<List<List<P1.Node>>> results;

    static {
        results = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            results.add(new ArrayList<>());
        }

        final List<List<P1.Node>> result0 = results.get(0);
        result0.add(Stream.of(3).map(P1.Node::new).collect(Collectors.toList()));
        result0.add(Stream.of(2, 1, 0).map(P1.Node::new).collect(Collectors.toList()));
        result0.add(Stream.of(4).map(P1.Node::new).collect(Collectors.toList()));

        results.get(1).add(Stream.of(5, 4, 3, 2, 1, 0).map(P1.Node::new).collect(Collectors.toList()));

        results.get(2).add(Stream.of(4, 3, 2, 1, 0).map(P1.Node::new).collect(Collectors.toList()));

        final List<List<P1.Node>> result3 = results.get(3);
        result3.add(Stream.of(7, 3, 4, 6, 5).map(P1.Node::new).collect(Collectors.toList()));
        result3.add(Stream.of(2, 1, 0).map(P1.Node::new).collect(Collectors.toList()));
        result3.add(Stream.of(8).map(P1.Node::new).collect(Collectors.toList()));
    }

    @Test
    public void stronglyConnectedComponents() throws Exception {
        Scanner scanner = new Scanner(new File(PATH));

        int test_count = scanner.nextInt();
        for (int i = 0; i < test_count; i++) {
            System.out.println("TEST " + (i + 1) + "\n");
            P1.Graph g = new P1.Graph(P1.Graph.GraphType.DIRECTED);
            g.readData(scanner);

            System.out.print(g);
            P1.StronglyConnectedComponents(g);
            System.out.println("###########################");

            assertEquals(results.get(i), g.getCtc());
        }
    }

}