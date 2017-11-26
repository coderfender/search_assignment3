package assignment3;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class AuthorRank {

	public static void main(String args[]) {
		String fileName = "/Users/bharvadl/Desktop/CourseWork/Search/author.net";

		DirectedSparseGraph<String, String> graph = new DirectedSparseGraph<String, String>();

		try (Scanner scanner = new Scanner(new File(fileName))) {

			Map<String, String> vertexMap = new HashMap<String, String>();

			String firstLine = scanner.nextLine();
			String[] firstLineWords = firstLine.split("\\s+");
			int numofVertices = Integer.parseInt(firstLineWords[firstLineWords.length - 1]);
			for (int i = 0; i < numofVertices; i++) {
				String s = scanner.nextLine();
				String[] split = s.split("\\s+");
				vertexMap.put(split[0], split[1].substring(1, split[1].length()-1));
				graph.addVertex(split[1].substring(1, split[1].length()-1));
			}
			
			
			Map<String, String> edgesMap = new HashMap<String, String>();

			String secondLine = scanner.nextLine();
			String[] secondLineWords = secondLine.split("\\s+");
			int numofEdges = Integer.parseInt(secondLineWords[secondLineWords.length - 1]);
			for (int i = 0; i < numofEdges; i++) {
				String s = scanner.nextLine();
				String[] split = s.split("\\s+");
				edgesMap.put(vertexMap.get(split[0]), vertexMap.get(split[1]));
				Pair<String> p = new Pair<String>(vertexMap.get(split[0]), vertexMap.get(split[1]));
				graph.addEdge(Integer.toString(i), p, EdgeType.DIRECTED);
			}

			double alpha = 0.1;
			PageRank<String, String> ranker = new PageRank<String, String>(graph, alpha);
			ranker.evaluate();

			Map<String, Double> result = new HashMap<String, Double>();
			for (String v : graph.getVertices()) {
				result.put(v, ranker.getVertexScore(v));
			}

			// Sort the results by descending order.
			result = sortByValue(result);
			// Get a set of the entries
			Set<Map.Entry<String, Double>> set = result.entrySet();
			// Get an iterator
			Iterator<Map.Entry<String, Double>> i = set.iterator();
			System.out.println("The top 10 ranked authors are: ");
			System.out.println("Author ID\tPage Rank Score");
			for (int j = 0; j < 10; j++) {
				Map.Entry<String, Double> me = (Map.Entry<String, Double>) i.next();
				System.out.println(me.getKey() /* + "\t\t"  + vertexMap.get(me.getKey()).toString() */ + "\t\t" + me.getValue());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
//	A comparator class just extend the existing map values of authors and ranks and display top 10

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

}