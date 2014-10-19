import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Model implements Serializable {
	private static final long serialVersionUID = 78987654356789L;

	private static final Tag[] tagsArray = {new Tag("CC"), new Tag("CD"), new Tag("DT"), new Tag("EX"), new Tag("FW"), 
		new Tag("IN"), new Tag("JJ"), new Tag("JJR"), new Tag("JJS"), new Tag("LS"), new Tag("MD"), new Tag("NN"), 
		new Tag("NNS"), new Tag("NNP"), new Tag("NNPS"), new Tag("PDT"), new Tag("POS"), new Tag("PRP"), new Tag("PRP$"), 
		new Tag("RB"), new Tag("RBR"), new Tag("RBS"), new Tag("RP"), new Tag("SYM"), new Tag("TO"), new Tag("UH"), 
		new Tag("VB"), new Tag("VBD"), new Tag("VBG"), new Tag("VBN"), new Tag("VBP"), new Tag("VBZ"), new Tag("WDT"), 
		new Tag("WP"), new Tag("WP$"), new Tag("WRB"), new Tag("$"), new Tag("#"), new Tag("``"), new Tag("''"), 
		new Tag("-LRB-"), new Tag("-RRB-"), new Tag(","), new Tag("."), new Tag(":")};

	private HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
	private HashMap<Tag, Integer> tagCount = new HashMap<Tag, Integer>();
	private HashMap<Tag, HashMap<String, Integer>> lexicalCount = new HashMap<Tag, HashMap<String, Integer>>();
   	private HashMap<Tag, Integer> singeltonCount = new HashMap<Tag, Integer>();
    private HashMap<Tag, Double> unigram = new HashMap<Tag, Double>();  
    private HashMap<Tag, HashMap<Tag, Double>> bigram = new  HashMap<Tag, HashMap<Tag, Double>>();
    private HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> trigram = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>>();
    private double lambda1;
    private double lambda2;
    private double lambda3;
    private int typeCount;
    private int tokenCount;

	public Model(HashMap<String, Integer> wordCount, HashMap<Tag, Integer> tagCount, 
		HashMap<Tag, HashMap<String, Integer>> lexicalCount, HashMap<Tag, Integer> singeltonCount, 
		HashMap<Tag, Double> unigram, HashMap<Tag, HashMap<Tag, Double>> bigram, 
		HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> trigram,
		double lambda1, double lambda2, double lambda3, int typeCount, int tokenCount) {
		
		this.wordCount = wordCount;
		this.tagCount = tagCount;
		this.lexicalCount = lexicalCount;
		this.singeltonCount = singeltonCount;
		this.unigram = unigram;
		this.bigram = bigram;
		this.trigram = trigram;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
		this.lambda3 = lambda3;
		this.typeCount = typeCount;
        this.tokenCount = tokenCount;
	}

	public double emissionProbability(Tag tag, String word) {
		int addOneNominator = (wordCount.get(word) != null) ? (wordCount.get(word) + 1) : 1;
		int addOneDenominator = tokenCount + typeCount;
		double addOneSmooth = (double) addOneNominator / addOneDenominator;
		double l = ((singeltonCount.get(tag) == null) ? 0 : singeltonCount.get(tag)) + 0.00000000000000000000000000001;
		double nominator = ((lexicalCount.get(tag) == null) ? 0 : (lexicalCount.get(tag).get(word) == null) ? 0 : lexicalCount.get(tag).get(word).intValue()) + addOneSmooth * l;
		int denominator = (tagCount.get(tag) == null) ? 0 : tagCount.get(tag) + ((singeltonCount.get(tag) == null) ? 0 : singeltonCount.get(tag));
		
		return (double) nominator / denominator;
	}

	public double transitionProbability(Tag tag1, Tag tag2, Tag tag3) {
		double unigramValue = (unigram.get(tag3) == null) ? 0.0 : unigram.get(tag3);
		double bigramValue = (bigram.get(tag2) == null) ? 0.0 : (bigram.get(tag2).get(tag3) == null) ? 0.0 : bigram.get(tag2).get(tag3);
		double trigramValue = (trigram.get(tag1) == null) ? 0.0 : (trigram.get(tag1).get(tag2) == null) ? 0.0 : (trigram.get(tag1).get(tag2).get(tag3) == null) ? 0.0 : trigram.get(tag1).get(tag2).get(tag3);
		
		return (lambda1 * unigramValue) + (lambda2 * bigramValue) + (lambda3 * trigramValue);
	}

	public Tag[] tag(String[] words) {

		Double[][] viberti = new Double[words.length][tagsArray.length];
		Tag[][] backTrace = new Tag[words.length - 1][tagsArray.length];

		for (int i = 0; i < tagsArray.length; i++) {
			viberti[0][i] = Math.log(transitionProbability(new Tag("<-1>"), new Tag("<0>"), tagsArray[i]));
			viberti[0][i] += Math.log(emissionProbability(tagsArray[i], words[0]));
		}

		// init
		if (words.length > 1) {
			for (int i = 0; i < tagsArray.length; i++) {
				double max = - Double.MAX_VALUE;
				Tag backPointer = new Tag();
				for (int j = 0; j < tagsArray.length; j++) {
					double val = Math.log(transitionProbability(new Tag("<0>"), tagsArray[j], tagsArray[i]));
					val += Math.log(emissionProbability(tagsArray[i], words[1]));
					val += viberti[0][j];
					if (val > max) {
						max = val;
						backPointer = tagsArray[j];
					}
				}
				viberti[1][i] = max;
				backTrace[0][i] = backPointer;
			}
		}

		// iter
		for (int iter = 2; iter < words.length; iter++) {
			for (int i = 0; i < tagsArray.length; i++) {
				double max = - Double.MAX_VALUE;
				Tag backPointer = new Tag();
				for (int j = 0; j < tagsArray.length; j++) {
					for (int k = 0; k < tagsArray.length; k++) {
						double val = Math.log(transitionProbability(tagsArray[k], tagsArray[j], tagsArray[i]));
						val += Math.log(emissionProbability(tagsArray[i], words[iter]));
						val += viberti[iter - 1][j];
						val += viberti[iter - 2][k];
						if (val > max) {
							max = val;
							backPointer = tagsArray[j];
						}
					}
				}
				viberti[iter][i] = max;
				backTrace[iter - 1][i] = backPointer;
			}
		}

		// terminate
		double max = - Double.MAX_VALUE;
		Tag backPointer = new Tag();
		for (int i = 0; i < tagsArray.length; i++) {
			double val = Math.log(bigram.get(tagsArray[i]).get(new Tag("<N+1>"))); 
			val += viberti[words.length - 1][i];
			if (val > max) {
				max = val;
				backPointer = tagsArray[i];
			}
		}

		// backtrack
		Tag[] path = new Tag[words.length];
		path[words.length - 1] = backPointer;
		for (int i = words.length - 2; i >= 0; i--) {
			int index = java.util.Arrays.asList(tagsArray).indexOf(backPointer);
			backPointer =  backTrace[i][index];
			path[i] = backPointer;
		}

		return path;
	}

   	private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
     	inputStream.defaultReadObject();
  	}

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
      	outputStream.defaultWriteObject();
    }
}
