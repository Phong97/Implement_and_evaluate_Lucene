package analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.StopwordAnalyzerBase;

public class CustomAnalyzer extends StopwordAnalyzerBase {
	/** Tokens longer than this length are discarded. Defaults to 50 chars. */
	public int maxTokenLength = 50;

	public CustomAnalyzer() {
		super(StandardAnalyzer.STOP_WORDS_SET);
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new StandardTokenizer();
		((StandardTokenizer) source).setMaxTokenLength(maxTokenLength);

		// normalizes tokens extracted with StandardTokenizer
		TokenStream result = new StandardFilter(source);

		// stripping 's from words
		result = new EnglishPossessiveFilter(result);

		// converts characters not in the (7-bit) ASCII range to the ASCII characters
		// that they resemble most closely; for example, it converts é to e
		result = new ASCIIFoldingFilter(result);

		// lowercase
		result = new LowerCaseFilter(result);

		// remove stopword
		result = new StopFilter(result, stopwords);

		// stemming using PorterStemmer
		result = new PorterStemFilter(result);

		// discard all tokens that have 2 or less characters
		result = new LengthFilter(result, 3, Integer.MAX_VALUE);

		return new TokenStreamComponents(source, result);
	}

}
