import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

public class build_tagger {
    private static File trainFile;
    private static File developmentFile;
    private static File modelFile;
    private static Boolean debug = false;

    private static int typeCount;
    private static int tokenCount;
    private static HashMap<String, Integer> wordCount = new HashMap<String, Integer>();

    // P(w_3 | t_3) = f(w_3, t_3) / f(t_3)
    private static HashMap<Tag, HashMap<String, Integer>> lexicalCount = new HashMap<Tag, HashMap<String, Integer>>();

    // P'(t_3) = f(t_3) / V  
    private static HashMap<Tag, Integer> unigramCount = new HashMap<Tag, Integer>();
    private static HashMap<Tag, Double> unigram = new HashMap<Tag, Double>();  
    
    // P'(t_3 | t_2) = f(t_2, t_3) / f(t_2)
    private static HashMap<Tag, HashMap<Tag, Integer>> bigramCount = new  HashMap<Tag, HashMap<Tag, Integer>>();
    private static HashMap<Tag, HashMap<Tag, Double>> bigram = new  HashMap<Tag, HashMap<Tag, Double>>();
    
    // P'(t_3 | t_1, t_2) = f(t_1, t_2, t_3) / f(t_1, t_2)
    private static HashMap<Tag, HashMap<Tag, HashMap<Tag, Integer>>> trigramCount = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Integer>>>();
    private static HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> trigram = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>>();
    
    // P(t_3 | t_2, t_1) = (lambda1 * unigram) + (lambda2 * bigram) + (lambda3 * trigram)
    private static double lambda1;
    private static double lambda2;
    private static double lambda3;

    private static Model model;

    static long startTime;
    static long endTime;

    /**
     * build_tagger.java
     * usage: java build_tagger <sents.train> <sents.devt> <model_file> [debug]
     */
    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        validateArguments(args);
        printTimer();
        train();
        printTimer();
        createModel();
        printTimer();
        test();
        printTimer();
        saveModel();
        printTimer();
        System.exit(0);
    }

    private static void validateArguments(String[] args) {
        if (args.length >= 4 && args[3].equalsIgnoreCase("debug")) {
            debug = true;
            System.out.println("debug mode has been enabled");
        }
        if (args.length >= 3) {
            trainFile = new File(args[0]);
            developmentFile = new File(args[1]);
            modelFile = new File(args[2]);
        } else {
            System.err.println("usage: java build_tagger <sents.train> <sents.devt> <model_file> [debug]");
            System.exit(1);
        }
    }

    private static void train() {
        try {
            if (debug)
                System.out.println("reading the train file: " + trainFile);
            
            BufferedReader reader = new BufferedReader(new FileReader(trainFile));
            
            String line;
            while ((line = reader.readLine()) != null) {
                Tag tag = new Tag();
                Tag tagPrev = new Tag("<t_(0)>");
                incrementCount(unigramCount, tagPrev);
                Tag tagPrevPrev = new Tag("<t_(-1)>");
                incrementCount(unigramCount, tagPrevPrev);
                incrementCount(bigramCount, tagPrevPrev, tagPrev);
                for (String tuple : line.split(" ")) {
                    int split = tuple.lastIndexOf('/');
                    String word = tuple.substring(0, split);
                    String tagString = tuple.substring(split + 1);
                    tag = new Tag(tagString);
                    
                    incrementCount(wordCount, word);
                    incrementCount(lexicalCount, tag, word);
                    
                    incrementCount(unigramCount, tag);
                    incrementCount(bigramCount, tagPrev, tag);
                    incrementCount(trigramCount, tagPrevPrev, tagPrev, tag);
                    
                    tagPrevPrev = tagPrev;
                    tagPrev = tag;
                }
                Tag tagEnd = new Tag("<t_(N+1)>");
                incrementCount(unigramCount, tagEnd);
                incrementCount(bigramCount, tag, tagEnd);
                incrementCount(trigramCount, tagPrevPrev, tag, tagEnd);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createModel() {
        typeCount = 0;
        tokenCount = 0;
        for (Integer value: wordCount.values()) {
            typeCount += 1;
            tokenCount += value;
        }
        calculateNgrams();
        calculateTransitionProbabilities();
        model = new Model(wordCount, unigramCount, lexicalCount, unigram, bigram, trigram, lambda1, lambda2, lambda3);
    }

    private static void calculateNgrams() {
        if (debug)        
            System.out.println("calculating n-grams...");

        // Calculate unigrams
        for (Entry<Tag, Integer> entry: unigramCount.entrySet()) {
            Tag tag = entry.getKey();
            Double value = (double) entry.getValue().intValue();
            unigram.put(tag, value / tokenCount);
        }

        // Calculate bigrams
        for (Entry<Tag, HashMap<Tag, Integer>> entry1: bigramCount.entrySet()) {
            Tag tag1 = entry1.getKey();
            HashMap<Tag, Integer> map1 = entry1.getValue();
            if (!bigram.containsKey(tag1))
                bigram.put(tag1, new HashMap<Tag, Double>());
            for (Entry<Tag, Integer> entry2: map1.entrySet()) {
                Tag tag2 = entry2.getKey();
                Double value = (double) entry2.getValue().intValue();
                bigram.get(tag1).put(tag2, value / unigramCount.get(tag2));
            }
        }

        // Calculate trigrams
        for (Entry<Tag, HashMap<Tag, HashMap<Tag, Integer>>> entry1: trigramCount.entrySet()) {
            Tag tag1 = entry1.getKey();
            HashMap<Tag, HashMap<Tag, Integer>> map1 = entry1.getValue();
            if (!trigram.containsKey(tag1))
                trigram.put(tag1, new HashMap<Tag, HashMap<Tag, Double>>());
            for (Entry<Tag, HashMap<Tag, Integer>> entry2: map1.entrySet()) {
                Tag tag2 = entry2.getKey();
                HashMap<Tag, Integer> map2 = entry2.getValue();
                if (!trigram.get(tag1).containsKey(tag2))
                    trigram.get(tag1).put(tag2, new HashMap<Tag, Double>());
                for (Entry<Tag, Integer> entry3: map2.entrySet()) {
                    Tag tag3 = entry3.getKey();
                    Double value = (double) entry3.getValue().intValue();
                    trigram.get(tag1).get(tag2).put(tag3, value / bigramCount.get(tag2).get(tag3));
                }
            }
        }
    }

    private static void calculateTransitionProbabilities() {
        if (debug)
            System.out.println("calculating transition probabilities...");

        lambda1 = 0.0;
        lambda2 = 0.0;
        lambda3 = 0.0;

        // Calculate lambdas
        for (Entry<Tag, HashMap<Tag, HashMap<Tag, Double>>> entry1: trigram.entrySet()) {
            Tag tag1 = entry1.getKey();
            HashMap<Tag, HashMap<Tag, Double>> map1 = entry1.getValue();
            for (Entry<Tag, HashMap<Tag, Double>> entry2: map1.entrySet()) {
                Tag tag2 = entry2.getKey();
                HashMap<Tag, Double> map2 = entry2.getValue();
                for (Entry<Tag, Double> entry3: map2.entrySet()) {
                    Tag tag3 = entry3.getKey();
                    double val1 = (unigramCount.get(tag3) - 0.999) / (tokenCount - 0.999);
                    double val2 = (bigramCount.get(tag2).get(tag3) - 0.999) / (unigramCount.get(tag2) - 0.999);
                    double val3 = (trigramCount.get(tag1).get(tag2).get(tag3) - 0.999) / (bigramCount.get(tag1).get(tag2) - 0.999);
                    
                    if (val1 > val2 && val1 > val3) 
                        lambda1 += trigramCount.get(tag1).get(tag2).get(tag3);
                    else if  (val2 > val3) 
                        lambda2 += trigramCount.get(tag1).get(tag2).get(tag3);
                    else
                        lambda3 += trigramCount.get(tag1).get(tag2).get(tag3);
                }
            }
        }
        
        // Normalize lambdas
        double lambdaSum = lambda1 + lambda2 + lambda3;
        lambda1 = lambda1 / lambdaSum;
        lambda2 = lambda2 / lambdaSum;
        lambda3 = lambda3 / lambdaSum;
    }

    private static void test() {
        String line;
        while ((line = new Scanner(System.in).nextLine()) != null)
            model.tag(line.split(" "));
        
        try {
            if (debug)
                System.out.println("reading the test file: " + developmentFile);
            BufferedReader reader = new BufferedReader(new FileReader(developmentFile));

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }    
    }

    private static void saveModel() {
        if (debug)        
            System.out.println("saving the model...");
        // TODO: Save the model
    }

    private static void incrementCount(HashMap<String, Integer> count, String key) {
        if (count.containsKey(key)) 
            count.put(key, 1 + count.get(key));
        else 
            count.put(key, 1);
    }
  
    private static void incrementCount(HashMap<Tag, Integer> count, Tag key) {
        if (!count.containsKey(key)) {
            count.put(key, 0);
        }
        count.put(key, 1 + count.get(key));
    }

    private static void incrementCount(HashMap<Tag, HashMap<String, Integer>> count, Tag key1, String key2) {
        if (!count.containsKey(key1)) {
            HashMap<String, Integer> newMap = new HashMap<String, Integer>();
            count.put(key1, newMap);
        }
        incrementCount(count.get(key1), key2);
    }

    private static void incrementCount(HashMap<Tag, HashMap<Tag, Integer>> count, Tag key1, Tag key2) {
        if (!count.containsKey(key1)) {
            HashMap<Tag, Integer> newMap = new HashMap<Tag, Integer>();
            count.put(key1, newMap);
        }
        incrementCount(count.get(key1), key2);
    }

    private static void incrementCount(HashMap<Tag, HashMap<Tag, HashMap<Tag, Integer>>> count, Tag key1, Tag key2, Tag key3) {
        if (!count.containsKey(key1)) {
            HashMap<Tag, HashMap<Tag, Integer>> newMap = new HashMap<Tag, HashMap<Tag, Integer>>();
            count.put(key1, newMap);
        }
        incrementCount(count.get(key1), key2, key3);
    }

    private static void printTimer() {
        System.out.println("timer > " + ((System.currentTimeMillis() - startTime) / 1000 ) + " seconds");
    }
}