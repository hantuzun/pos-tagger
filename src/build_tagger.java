import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class build_tagger {
    private static File trainFile;
    private static File developmentFile;
    private static File modelFile;
    private static Boolean debug = false;

    // P(w_3 | t_3) = f(w_3, t_3) / f(t_3)
    private static HashMap<Tag, Integer> tagCount = new HashMap<Tag, Integer>();
    private static HashMap<Tag, HashMap<String, Integer>> lexicalCount = new HashMap<Tag, HashMap<String, Integer>>();
    private static HashMap<Tag, HashMap<String, Double>> emissionProbability = new HashMap<Tag, HashMap<String, Double>>();

    // P'(t_3) = f(t_3) / V  
    private static HashSet<String> vocabulary = new HashSet<String>();
    private static HashMap<Tag, Double> unigram = new HashMap<Tag, Double>();  
    
    // P'(t_3 | t_2) = f(t_3 | t_2) / f(t_2)
    private static HashMap<Tag, HashMap<Tag, Integer>> bigramCount = new  HashMap<Tag, HashMap<Tag, Integer>>();
    private static HashMap<Tag, HashMap<Tag, Double>> bigram = new  HashMap<Tag, HashMap<Tag, Double>>();
    
    // P'(t_3 | t_2, t_1) = f(t_1, t_2, t_3) / f(t_1, t_2)
    private static HashMap<Tag, HashMap<Tag, HashMap<Tag, Integer>>> trigramCount = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Integer>>>();
    private static HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> trigram = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>>();
    
    // P(t_3 | t_1, t_2) = (lambda_1 * unigram) + (lambda_2 * bigram) + (lambda_3 8 trigram)
    private static double lambda_1;
    private static double lambda_2;
    private static double lambda_3;
    private static HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> transitionProbability = new HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>>();

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
        develop();
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
                Tag tagPrev = new Tag("<t_(0)>");
                Tag tagPrevPrev = new Tag("<t_(-1)>");
                Tag tag = new Tag();
                for (String tuple : line.split(" ")) {
                    int split = tuple.lastIndexOf('/');
                    String word = tuple.substring(0, split);
                    String tagString = tuple.substring(split + 1);
                    tag = new Tag(tagString);
                    
                    incrementCount(lexicalCount, tag, word);
                    
                    incrementCount(tagCount, tag);
                    incrementCount(bigramCount, tagPrev, tag);
                    incrementCount(trigramCount, tagPrevPrev, tagPrev, tag);
                    
                    tagPrevPrev = tagPrev;
                    tagPrev = tag;
                }
                Tag tagEnd = new Tag("<t_(N+1)>");
                incrementCount(bigramCount, tagEnd, tag);
                incrementCount(trigramCount, tagEnd, tag, tagPrev);
            }

            if (debug) {
                System.out.println("distinct NN count: " + lexicalCount.get(new Tag("NN")).size());
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (debug)        
            System.out.println("n-grams are calculating...");
        // TODO: Calculate n-grams
    }

    private static void develop() {
        try {
            if (debug)
                System.out.println("reading the development file: " + developmentFile);
            BufferedReader reader = new BufferedReader(new FileReader(developmentFile));

            if (debug)
                System.out.println("calculating emission and transition probabilities");

            // TODO: Calculate lambda values        
            // TODO: Calculate emissionProbability
            // TODO: Calculate transitionProbability

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }    
    }

    private static void saveModel() {
        if (debug)        
            System.out.println("creating the model...");
        model = new Model(transitionProbability, emissionProbability);
        
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