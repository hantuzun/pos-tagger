import java.io.Serializable;
import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

public class Model implements Serializable {
    private static final HashSet<String> tagStrings = new HashSet<String>(Arrays.asList("<t_(-1)>", "<t_(0)>", "CC", 
    "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS",
    "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", 
    "WDT", "WP", "WP$", "WRB", "$", "#", "``", "''", "-LRB-", "-RRB-", ",", ".", ":", "<t_(N+1)>"));
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
	}

	public double emissionProbability(Tag tag, String word) {
		int addOneNominator = (wordCount.get(word) != null) ? (wordCount.get(word) + 1) : 1;
		int addOneDenominator = tokenCount + typeCount;
		double addOneSmooth = (double) addOneNominator / addOneDenominator;
		double nominator = ((lexicalCount.get(tag) == null) ? 0 : (lexicalCount.get(tag).get(word) == null) ? 0 : lexicalCount.get(tag).get(word).intValue()) + addOneSmooth * ((singeltonCount.get(tag) == null) ? 0 : singeltonCount.get(tag));
		int denominator = (tagCount.get(tag) == null) ? 0 : tagCount.get(tag) + ((singeltonCount.get(tag) == null) ? 0 : singeltonCount.get(tag));
		
		return (double) nominator / denominator;
	}

	public double transitionProbability(Tag tag1, Tag tag2, Tag tag3) {
		double unigramValue = (unigram.get(tag3) == null) ? 0.0 : unigram.get(tag3);
		double bigramValue = (bigram.get(tag2) == null) ? 0.0 : (bigram.get(tag2).get(tag3) == null) ? 0.0 : bigram.get(tag2).get(tag3);
		double trigramValue = (trigram.get(tag1) == null) ? 0.0 : (trigram.get(tag1).get(tag2) == null) ? 0.0 : (trigram.get(tag1).get(tag2).get(tag3) == null) ? 0.0 : trigram.get(tag1).get(tag2).get(tag3);
		
		return (lambda1 * unigramValue) + (lambda2 * bigramValue) + (lambda3 * trigramValue);
	}

	public void tag(String[] words) {
		int len = words.length;
		HashMap<Tag, Double>[] wordToTag = new HashMap[len];
		HashMap<Tag, Tag>[] backtrace = new HashMap[len];
		
		// Initialization
		if (len >= 1) {
			wordToTag[0] = new HashMap<Tag, Double>();
			for (Tag tag: tags) {
				wordToTag[0].put(tag, transitionProbability(new Tag("<t_(-1)>"), new Tag("<t_(0)>"), tag) * emissionProbability(tag, words[0]));
			}	
		}

		if (len >= 2) {
			wordToTag[1] = new HashMap<Tag, Double>();
			backtrace[0] = new HashMap<Tag, Tag>();
			for (Tag tag: tags) {
				double max = Double.MIN_VALUE;
				Tag backPointer = new Tag();
				for (Tag tagPrev: tags) {
					double val = transitionProbability(new Tag("<t_(0)>"), tagPrev, tag) * emissionProbability(tag, words[0]) * wordToTag[0].get(tagPrev);
					if (val > max) {
						max = val;
						backPointer = tagPrev;
					}
				}
				wordToTag[1].put(tag, max);
				backtrace[0].put(tag, backPointer);
			}	
		}

		// Iteration
		// TODO: 	add  emissionProbability(tag, words[0]) for iteration
		// double val = transitionProbability(tagPrevPrev, tagPrev, tag) * wordToTag[i - 1].get(tagPrev) * wordToTag[i - 2].get(tagPrevPrev);
				

		// Termination
		if (len >= 2) {
			int i = len;
			backtrace[i - 1] = new HashMap<Tag, Tag>();
			Tag tag = new Tag("<t_(N+1)>");
			Tag backPointer = new Tag();
			double max = Double.MIN_VALUE;
			for (Tag tagPrev: tags) {
				for (Tag tagPrevPrev: tags) {
					double val = transitionProbability(tagPrevPrev, tagPrev, tag) * wordToTag[i - 1].get(tagPrev) * wordToTag[i - 2].get(tagPrevPrev);
					if (val > max) {
						max = val;
						backPointer = tagPrev;
					}
				}
			}
			backtrace[len - 1].put(tag, backPointer);
		}

		// Backtracking
		Tag backPointer = new Tag("<t_(N+1)>");
		for (int i = len - 1; i >= 0; i--) {
			backPointer = backtrace[i].get(backPointer);
			System.out.println(backPointer);
		}
	}
}
