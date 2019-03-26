package search;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.FSDirectory;

/** Simple command-line based search demo. */
public class TrecBatchSearch {

	private TrecBatchSearch() {
	}

	/** Simple command-line based search demo. */
	public static void main(String[] args) throws Exception {
		String usage = "Usage:\tjava TrecBatchSearch [-index dir] [-simfn similarity] [-field f] [-queries file]";
		if (args.length > 0 && ("-h".equals(args[0]) || "-help".equals(args[0]))) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:\ndefault: DefaultSimilary (tfidf)\n");
			System.exit(0);
		}

		String index = "index";
		String field = "contents";
		String queries = null;
		String simstring = "classic";

		for (int i = 0; i < args.length; i++) {
			if ("-index".equals(args[i])) {
				index = args[i + 1];
				i++;
			} else if ("-field".equals(args[i])) {
				field = args[i + 1];
				i++;
			} else if ("-queries".equals(args[i])) {
				queries = args[i + 1];
				i++;
			} else if ("-simfn".equals(args[i])) {
				simstring = args[i + 1];
				i++;
			}
		}

		Similarity simfn = null;
		if ("classic".equals(simstring)) {
			simfn = new ClassicSimilarity();
		} else if ("bm25".equals(simstring)) {
			// The model has two parameters, k1 and b.
			// Default value: k1 = 1.2, b = 0.75.
			simfn = new BM25Similarity(1.2f, 0.75f);
		} else if ("dfr".equals(simstring)) {
			/*
			 * The model has three parameters, BasicModel, AfterEffect and Normalization
			 * BasicModel: Basic model of information content: BasicModelBE: Limiting form
			 * of Bose-Einstein BasicModelG: Geometric approximation of Bose-Einstein
			 * BasicModelP: Poisson approximation of the Binomial BasicModelD: Divergence
			 * approximation of the Binomial BasicModelIn: Inverse document frequency
			 * BasicModelIne: Inverse expected document frequency [mixture of Poisson and
			 * IDF] BasicModelIF: Inverse term frequency [approximation of I(ne)]
			 * AfterEffect: First normalization of information gain: AfterEffectL: Laplace's
			 * law of succession AfterEffectB: Ratio of two Bernoulli processes
			 * AfterEffect.NoAfterEffect: no first normalization Normalization: Second
			 * (length) normalization: NormalizationH1: Uniform distribution of term
			 * frequency NormalizationH2: term frequency density inversely related to length
			 * NormalizationH3: term frequency normalization provided by Dirichlet prior
			 * NormalizationZ: term frequency normalization provided by a Zipfian relation
			 * Normalization.NoNormalization: no second normalization
			 */
			simfn = new DFRSimilarity(new BasicModelP(), new AfterEffectL(), new NormalizationH2());
		} else if ("lmdrichlet".equals(simstring)) {
			// The model has a single parameter, μ.
			// Default value: μ = 2000
			simfn = new LMDirichletSimilarity();
		} else if ("lmjelinekmercer".equals(simstring)) {
			// The model has a single parameter, λ.
			// The optimal value is around 0.1 for title queries and 0.7 for long queries.
			simfn = new LMJelinekMercerSimilarity(0.1f);
		} else if ("ib".equals(simstring)) {
			/*
			 * The model has three parameters, Distribution, Lambda and Normalization
			 * Distribution: Probabilistic distribution used to model term occurrence
			 * DistributionLL: Log-logistic DistributionLL: Smoothed power-law Lambda: λw
			 * parameter of the probability distribution LambdaDF: Nw/N or average number of
			 * documents where w occurs LambdaTTF: Fw/N or average number of occurrences of
			 * w in the collection Normalization: Term frequency normalization
			 */
			simfn = new IBSimilarity(new DistributionLL(), new LambdaDF(), new NormalizationH2());
		}
		if (simfn == null) {
			System.out.println(usage);
			System.out.println("Supported similarity functions:");
			System.out.println("\tclassic: ClassicSimilary (tfidf)");
			System.out.println("\tbm25: BM25Similarity (standard parameters)");
			System.out.println("\tdfr: Divergence from Randomness model (PL2 variant)");
			System.out.println("\tlmdrichlet: Language model, Dirichlet smoothing");
			System.out.println("\tlmjelinekmercer: Language model, Jelinek-Mercer smoothing");
			System.out.println("\tib: information-based model");
			System.exit(0);
		}

		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		searcher.setSimilarity(simfn);
		Analyzer analyzer = new StandardAnalyzer();

		BufferedReader in = null;
		if (queries != null) {
			in = new BufferedReader(new InputStreamReader(new FileInputStream(queries), "UTF-8"));
		} else {
			in = new BufferedReader(new InputStreamReader(new FileInputStream("queries"), "UTF-8"));
		}
		QueryParser parser = new QueryParser(field, analyzer);
		String filename = "results_" + simstring;
		FileWriter fileWriter = new FileWriter(filename);
		PrintWriter printWriter = new PrintWriter(fileWriter);
		String line;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.length() == 0) {
				continue;
			}

			String[] pair = line.split(" ", 2);
			Query query = parser.parse(pair[1]);

			doBatchSearch(in, searcher, pair[0], query, simstring, printWriter);
		}
		reader.close();
		printWriter.close();
	}

	/**
	 * This function performs a top-1000 search for the query as a basic TREC run.
	 */
	public static void doBatchSearch(BufferedReader in, IndexSearcher searcher, String qid, Query query,
			String algorithm, PrintWriter printWriter) throws IOException {

		// Collect enough docs to show 5 pages
		TopDocs results = searcher.search(query, 1000);
		ScoreDoc[] hits = results.scoreDocs;
		HashMap<String, String> seen = new HashMap<String, String>(1000);
		long numTotalHits = results.totalHits;

		int start = 0;
		long end = Math.min(numTotalHits, 1000);
		for (int i = start; i < end; i++) {
			Document doc = searcher.doc(hits[i].doc);
			String docno = doc.get("docno");
			// There are duplicate document numbers in the FR collection,
			// so only output a given docno once.
			if (seen.containsKey(docno)) {
				continue;
			}
			seen.put(docno, docno);

			System.out.println(qid + " 0 " + docno + " " + i + " " + hits[i].score + " " + algorithm);
			printWriter.printf(qid + " 0 " + docno + " " + i + " " + hits[i].score + " " + algorithm + "\n");
		}
	}
}
