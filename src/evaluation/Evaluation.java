package evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import utils.SearchResult;

public class Evaluation {
	public static void main(String[] args) {
		try {
			// path of the search results folder
			String pathResults = "C:\\Users\\phong\\eclipse-workspace\\lab4";
			// path of the qrels file
			String pathQrels = "./trec_queries_qrels/trec.qrels.301-450";

			Set<String> relDocnos = new TreeSet<>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pathQrels), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				String[] splits = line.split("\\s+");
				if (splits[1].equals("1")) {
					relDocnos.add(splits[0]);
				}
			}
			reader.close();

			System.out.printf("%-15s%-6s%-6s%-6s%-6s%-6s%-6s%-6s\n", 
					"MODEL", "P@5", "P@10", "P@20", "R@10", "R@20", "R@100", "AP");
			String[] algorithms = new String[] {"classic", "bm25", "dfr", "lmdrichlet", "lmjelinekmercer", "ib"};

			for (String algorithm : algorithms) {
				List<SearchResult> results = SearchResult
						.readTRECFormat(new File(new File(pathResults), "results_" + algorithm)).get("0");
				double p5 = evalPrecision(results, relDocnos, 5); // Precision at rank 5
				double p10 = evalPrecision(results, relDocnos, 10); // Precision at rank 10
				double p20 = evalPrecision(results, relDocnos, 20); // Precision at rank 20
				double r10 = evalRecall(results, relDocnos, 10); // Recall at rank 10
				double r20 = evalRecall(results, relDocnos, 20); // Recall at rank 20
				double r100 = evalRecall(results, relDocnos, 100); // Recall at rank 100
				double ap = evalAP(results, relDocnos);

				
				// Write your code to load query file, search and write results to file
				// Output format: <algorithm> <p5> <p10> <p20> <r10> <r20> <r100> <ap>
				 System.out.printf( "%-15s%-6.2f%-6.2f%-6.2f%-6.2f%-6.2f%-6.2f%-6.2f\n",
				 algorithm, p5, p10, p20, r10, r20, r100, ap );

				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Compute precision at rank k.
	 *
	 * @param results
	 *            A ranked list of search results.
	 * @param relDocnos
	 *            The set of relevant documents.
	 * @param k
	 *            The cutoff rank k.
	 * @return
	 */
	public static double evalPrecision(List<SearchResult> results, Set<String> relDocnos, int k) {
		// write your implementation for "P@k" here
		long rrk = 0;
		results = results.subList(0, k);
		for (SearchResult result : results) {
			if (relDocnos.contains(result.getDocno())) {
				rrk++;
			}
		}
		return (rrk*1.0)/k;
	}

	/**
	 * Compute recall at rank k.
	 *
	 * @param results
	 *            A ranked list of search results.
	 * @param relDocnos
	 *            The set of relevant documents.
	 * @param k
	 *            The cutoff rank k.
	 * @return
	 */
	public static double evalRecall(List<SearchResult> results, Set<String> relDocnos, int k) {
		// write your implementation for "Recall@k" here
		long rrk = 0;
		results = results.subList(0, k);
		for (SearchResult result : results) {
			if (relDocnos.contains(result.getDocno())) {
				rrk++;
			}
		}
		
		return (rrk*1.0)/relDocnos.size();
	}

	/**
	 * Compute the average precision of the whole ranked list.
	 *
	 * @param results
	 *            A ranked list of search results.
	 * @param relDocnos
	 *            The set of relevant documents.
	 * @return
	 */
	public static double evalAP(List<SearchResult> results, Set<String> relDocnos) {
		// write your implementation for "AP" here
		double ap = 0;
		for (int i = 1; i< results.size(); i++) {
			if (relDocnos.contains(results.get(i).getDocno())) {
				List<SearchResult> reti = results.subList(0, i);
				ap += evalPrecision(reti, relDocnos, i);
			}
		}
		return ap/relDocnos.size();
	}

	/**
	 * Load a TREC-format relevance judgment (qrels) file.
	 *
	 * @param f
	 *            A qrels file.
	 * @return A map storing the set of relevant documents for each qid.
	 * @throws IOException
	 */
	public static Map<String, Set<String>> loadQrels(File f) throws IOException {
		Map<String, Set<String>> qrels = new TreeMap<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] splits = line.split("\\s+");
			String qid = splits[0];
			String docno = splits[2];
			qrels.putIfAbsent(qid, new TreeSet<>());
			if (Integer.parseInt(splits[3]) > 0) {
				qrels.get(qid).add(docno);
			}
		}
		reader.close();
		return qrels;
	}

	/**
	 * Load a query file.
	 *
	 * @param f
	 *            A query file.
	 * @return A map storing the text query for each qid.
	 * @throws IOException
	 */
	public static Map<String, String> loadQueries(String f) throws IOException {
		return loadQueries(new File(f));
	}

	/**
	 * Load a query file.
	 *
	 * @param f
	 *            A query file.
	 * @return A map storing the text query for each qid.
	 * @throws IOException
	 */
	public static Map<String, String> loadQueries(File f) throws IOException {
		Map<String, String> queries = new TreeMap<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] splits = line.split("\t");
			String qid = splits[0];
			String query = splits[1];
			queries.put(qid, query);
		}
		reader.close();
		return queries;
	}

}
