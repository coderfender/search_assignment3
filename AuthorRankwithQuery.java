package assignment3;

import java.io.File;
import java.io.IOException;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.MapTransformer;
import org.apache.lucene.analysis.Analyzer;

//Using Older Lucene version 4.0

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;


import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;

public class AuthorRankwithQuery {

	public static String index = "/Users/bharvadl/Desktop/CourseWork/Search/author_index/";
	public static String authorFile = "/Users/bharvadl/Desktop/CourseWork/Search/author.net";
	public static Map<String, Double> mapAuthors;

	public static void priorCompute(String queryString) {
		try {
			IndexReader idxReader = DirectoryReader.open(FSDirectory.open(new File(index)));
			IndexSearcher idxSearcher = new IndexSearcher(idxReader);
			Analyzer luceneAnalyzer = new StandardAnalyzer();
			idxSearcher.setSimilarity(new BM25Similarity());

			QueryParser parser = new QueryParser("content", luceneAnalyzer);
			Query query = parser.parse(queryString);
			System.out.println("Searching the Query: " + query.toString("content"));

			TopDocs results = idxSearcher.search(query, 300);

//			 Print number of hits over all the documents
			int numTotalHits = results.totalHits;
			System.out.println(" Total matching documents found:" + numTotalHits +  "\n");

			// Print retrieved results
			ScoreDoc[] hits = results.scoreDocs;

			mapAuthors = new HashMap<String, Double>();

			double priorSum = 0;
			for (int i = 0; i < hits.length; i++) {
				Document doc = idxSearcher.doc(hits[i].doc);
				priorSum += hits[i].score;
				if (mapAuthors.containsKey(doc.get("authorid"))) {
					// key exists that is author is already seen in previous documents.Just update the score
					Double old = mapAuthors.get(doc.get("authorid"));
					double sum = hits[i].score + old.doubleValue();
					mapAuthors.put(doc.get("authorid"), new Double(sum));
				} else {
					// Key does not exist.We are seeing the author for the first time.
					mapAuthors.put(doc.get("authorid"), new Double(hits[i].score));
				}
			}
			// Get a author_set of the entries of all the authors into a unique author_set (as no duplicates allowed).
			Set<Map.Entry<String, Double>> author_set = mapAuthors.entrySet();
			// Get an iterator to traverse through the map
			Iterator<Map.Entry<String, Double>> i = author_set.iterator();
			// Display elements
			while (i.hasNext()) {
				Map.Entry<String, Double> me = (Map.Entry<String, Double>) i.next();
				double value = me.getValue().doubleValue();
				value /= priorSum;
				mapAuthors.put(me.getKey(), value);
			}
			idxReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {

		String queryString = "Information Retrieval";
		priorCompute(queryString);

		DirectedSparseGraph<String, String> graph = new DirectedSparseGraph<String, String>();

		try (Scanner scanner = new Scanner(new File(authorFile))) {

			Map<String, String> verticesMap = new HashMap<String, String>();

			String firstLine = scanner.nextLine();
			String[] firstLineWords = firstLine.split("\\s+");
			int numofVertices = Integer.parseInt(firstLineWords[firstLineWords.length - 1]);
			for (int i = 0; i < numofVertices; i++) {
				String s = scanner.nextLine();
				String[] split = s.split("\\s+");
				verticesMap.put(split[0], split[1].substring(1, split[1].length()-1));
				graph.addVertex(split[1].substring(1, split[1].length()-1));
				if(mapAuthors.containsKey(split[1].substring(1, split[1].length()-1)) == false)
				{
					mapAuthors.put(split[1].substring(1, split[1].length()-1), new Double(0.0));
				}
			}

			Map<String, String> edgesMap = new HashMap<String, String>();

			String secondLine = scanner.nextLine();
			String[] secondLineWords = secondLine.split("\\s+");
			int numofEdges = Integer.parseInt(secondLineWords[secondLineWords.length - 1]);
			for (int i = 0; i < numofEdges; i++) {
				String s = scanner.nextLine();
				String[] split = s.split("\\s+");
				// System.out.println(split[0]+" " + split[1] + " " + split[2]);
				edgesMap.put(verticesMap.get(split[0]), verticesMap.get(split[1]));
				Pair<String> p = new Pair<String>(verticesMap.get(split[0]), verticesMap.get(split[1]));
				graph.addEdge(Integer.toString(i), p, EdgeType.DIRECTED);
			}

			double alpha = 0.1;
			Transformer<String, Double> mapAuthorsTransformer = MapTransformer.getInstance(mapAuthors);			
			PageRankWithPriors<String, String> ranker = new PageRankWithPriors<String, String>(graph,
					mapAuthorsTransformer, alpha);
			ranker.evaluate();

			Map<String, Double> result = new HashMap<String, Double>();
			for (String v : graph.getVertices()) {
				result.put(v, ranker.getVertexScore(v));
			}

			// Sort the results by descending order.
			result = sortByValue(result);
			// Get a author_set of the entries
			Set<Map.Entry<String, Double>> author_set = result.entrySet();
			// Get an iterator
			Iterator<Map.Entry<String, Double>> i = author_set.iterator();
			System.out.println("The top 10 ranked authors for query \"" + queryString +  "\" are: ");
			System.out.println("Author ID\tPage Rank Score");
			for (int j = 0; j < 10; j++) {
				Map.Entry<String, Double> me = (Map.Entry<String, Double>) i.next();
				System.out.println(me.getKey()
						/* + "\t\t" + verticesMap.get(me.getKey()).toString() */ + "\t\t" + me.getValue());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}
}
