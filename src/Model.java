import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

public class Model implements Serializable {
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
	}

	public double emissionProbability(Tag tag, String word) {
		int addOneNominator = (wordCount.get(word) != null) ? (wordCount.get(word) + 1) : 1;
		int addOneDenominator = tokenCount + typeCount;
		double addOneSmooth = (double) addOneNominator / addOneDenominator;
		double nominator = lexicalCount.get(tag).get(word).intValue() + addOneSmooth * singeltonCount.get(tag);
		double denominator = tagCount.get(tag) + singeltonCount.get(tag);
		
		return nominator / denominator;
	}

	public double transitionProbability(Tag tag1, Tag tag2, Tag tag3) {
		double unigramValue = (unigram.get(tag3) == null) ? 0 : unigram.get(tag3);
		double bigramValue = (bigram.get(tag2).get(tag3) == null) ? 0 : bigram.get(tag2).get(tag3);
		double trigramValue = (trigram.get(tag1).get(tag2).get(tag3) == null) ? 0 : trigram.get(tag1).get(tag2).get(tag3);
		
		return (lambda1 * unigramValue) + (lambda2 * bigramValue) + (lambda3 * trigramValue);
	}
}
