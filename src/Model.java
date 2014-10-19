import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Model implements Serializable {
    private static final HashSet<String> tagStrings = new HashSet<String>(Arrays.asList("CC", 
    "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS",
    "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", 
    "WDT", "WP", "WP$", "WRB", "$", "#", "``", "''", "-LRB-", "-RRB-", ",", ".", ":"));
	private static final Tag[] tagArray = new Tag[tagStrings.size()];
    private static final HashSet<Tag> tags = new HashSet<Tag>();

   	private HashMap<Tag, Integer> singeltonCount = new HashMap<Tag, Integer>();

	private HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
	private HashMap<Tag, Integer> tagCount = new HashMap<Tag, Integer>();
	private HashMap<Tag, HashMap<String, Integer>> lexicalCount = new HashMap<Tag, HashMap<String, Integer>>();
    private HashMap<Tag, Double> unigram = new HashMap<Tag, Double>();  
    private HashMap<Tag, HashMap<Tag, Double>> bigram = new  HashMap<Tag, HashMap<Tag, Double>>();
    private HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> trigram = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>>();
    private int typeCount;
    private int tokenCount;
    private double lambda1;
    private double lambda2;
    private double lambda3;

	public Model(HashMap<String, Integer> wordCount, HashMap<Tag, Integer> tagCount, 
		HashMap<Tag, HashMap<String, Integer>> lexicalCount, HashMap<Tag, Double> unigram,
		HashMap<Tag, HashMap<Tag, Double>> bigram, HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> trigram,
		double lambda1, double lambda2, double lambda3) {
		
		this.wordCount = wordCount;
		this.tagCount = tagCount;
		this.lexicalCount = lexicalCount;
		this.unigram = unigram;
		this.bigram = bigram;
		this.trigram = trigram;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
		this.lambda3 = lambda3;

		typeCount = 0;
        tokenCount = 0;    
        for (Integer value: wordCount.values()) {
            typeCount += 1;
            tokenCount += value;
        }

        for (Entry<Tag, HashMap<String, Integer>> entry: lexicalCount.entrySet()) {
        	Tag tag = entry.getKey();
        	HashMap<String, Integer> wordMap = entry.getValue();
        	int singelton = 0;
        	for (Integer count: wordMap.values())
				if (count == 1)
					singelton++;
        	singeltonCount.put(tag, singelton);
		}	

		for (String tagString: tagStrings) {
			tags.add(new Tag(tagString));
		}

		int index = 0;
        for (Tag tag: tags) {
        	tagArray[index++] = tag;
        }
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
		int len = words.length;
		Double[][] viberti = new Double[len][tags.size()];
		Tag[][] backTrace = new Tag[len - 1][tags.size()];

		for (int i = 0; i < tags.size(); i++) {
			viberti[0][i] = Math.log(transitionProbability(new Tag("<-1>"), new Tag("<0>"), tagArray[i]));
			viberti[0][i] += Math.log(emissionProbability(tagArray[i], words[0]));
		}

		// init
		if (len > 1) {
			for (int i = 0; i < tags.size(); i++) {
				double max = - Double.MAX_VALUE;
				Tag backPointer = new Tag();
				for (int j = 0; j < tags.size(); j++) {
					double val = Math.log(transitionProbability(new Tag("<0>"), tagArray[j], tagArray[i]));
					val += Math.log(emissionProbability(tagArray[i], words[1]));
					val += viberti[0][j];
					if (val > max) {
						max = val;
						backPointer = tagArray[j];
					}
				}
				viberti[1][i] = max;
				backTrace[0][i] = backPointer;
			}
		}

		// iter
		for (int iter = 2; iter < len; iter++) {
			for (int i = 0; i < tags.size(); i++) {
				double max = - Double.MAX_VALUE;
				Tag backPointer = new Tag();
				for (int j = 0; j < tags.size(); j++) {
					for (int k = 0; k < tags.size(); k++) {
						double val = Math.log(transitionProbability(tagArray[k], tagArray[j], tagArray[i]));
						val += Math.log(emissionProbability(tagArray[i], words[iter]));
						val += viberti[iter - 1][j];
						val += viberti[iter - 2][k];
						if (val > max) {
							max = val;
							backPointer = tagArray[j];
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
		for (int i = 0; i < tags.size(); i++) {

			double val = Math.log(bigram.get(tagArray[i]).get(new Tag("<N+1>"))); 
			val += viberti[len - 1][i];
			if (val > max) {
				max = val;
				backPointer = tagArray[i];
			}
		}		

		// backtrack
		Tag[] path = new Tag[len];
		int p = 0;
		path[len - 1] = backPointer;
		for (int i = len - 2; i >= 0; i--) {
			int index = java.util.Arrays.asList(tagArray).indexOf(backPointer);
			backPointer =  backTrace[i][index];

			path[i] = backPointer;
		}

		return path;
	}
}
