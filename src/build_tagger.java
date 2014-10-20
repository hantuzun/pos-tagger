import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

    private static HashMap<String, Integer> wordCount = new HashMap<String, Integer>();

    private static HashMap<Tag, Integer> singeltonCount = new HashMap<Tag, Integer>();

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

    private static int typeCount;
    private static int tokenCount;
    
    private static Model model;

    private static HashMap<Tag, Integer> truePositives = new HashMap<Tag, Integer>();
    private static HashMap<Tag, Integer> falsePositives = new HashMap<Tag, Integer>();
    private static HashMap<Tag, Integer> falseNegatives = new HashMap<Tag, Integer>();
    private static HashMap<Tag, Double> precision = new HashMap<Tag, Double>();
    private static HashMap<Tag, Double> accuracy = new HashMap<Tag, Double>();
    private static HashMap<Tag, Double> f1Score = new HashMap<Tag, Double>();

    private static int tp;
    private static int fp;
    private static int fn;

    /**
     * build_tagger.java
     * usage: java build_tagger <sents.train> <sents.devt> <model_file>
     */
    public static void main(String[] args) {
        validateArguments(args);
        train();
        createModel();
        System.out.println("Model created");
        saveModel(args);
        System.out.println("Model saved");
        System.out.println("Testing the development data...");
        test();
        System.out.println("Testing complete");
        printStats();
        System.exit(0);
    }

    private static void printStats() {
        tp = 0;
        fp = 0;
        fn = 0;
        for (String str: new Tag().getTags()) {
            Tag tag = new Tag(str);
            tp += truePositives.get(tag);
            fp += falsePositives.get(tag);
            fn += falseNegatives.get(tag);
            precision.put(tag, (double) truePositives.get(tag) / (truePositives.get(tag) + falsePositives.get(tag)));
            accuracy.put(tag, (double) truePositives.get(tag) / (truePositives.get(tag) + falseNegatives.get(tag)));
            f1Score.put(tag, 2 * (precision.get(tag) + accuracy.get(tag)) / (precision.get(tag) * accuracy.get(tag)));
        }

        for (String str: new Tag().getTags()) {
            Tag tag = new Tag(str);
            System.out.print(tag + "  ");
            System.out.printf("\t %4d", truePositives.get(tag));
            System.out.printf("\t %4d", falsePositives.get(tag));
            System.out.printf("\t %4d", falseNegatives.get(tag));
            System.out.printf("\t %.4f", precision.get(tag));
            System.out.printf("\t %.4f", accuracy.get(tag));
            System.out.printf("\t %2.4f", f1Score.get(tag));
            System.out.println();
        }   

        System.out.println();
        double pre = (double) tp / (tp + fp);
        double acc = (double) tp / (tp + fn);
        double f1 = 2 * (acc + pre) / (acc * pre);
        System.out.println(tp + " " + fp + " " + fn + " " + pre + " " + acc + " " + f1);
    }

    private static void validateArguments(String[] args) {
        if (args.length >= 3) {
            trainFile = new File(args[0]);
            developmentFile = new File(args[1]);
            modelFile = new File(args[2]);
        } else {
            System.err.println("usage: java build_tagger <sents.train> <sents.devt> <model_file>");
            System.exit(1);
        }
    }

    private static void train() {
        try {            
            BufferedReader reader = new BufferedReader(new FileReader(trainFile));
            
            try {            
                String line;
                while ((line = reader.readLine()) != null) {
                    Tag tag = new Tag();
                    Tag tagPrev = new Tag("<0>");
                    incrementCount(unigramCount, tagPrev);
                    Tag tagPrevPrev = new Tag("<-1>");
                    incrementCount(unigramCount, tagPrevPrev);
                    incrementCount(bigramCount, tagPrevPrev, tagPrev);
                    for (String tuple : line.split(" ")) {
                        int split = tuple.lastIndexOf('/');
                        String word = tuple.substring(0, split).replaceAll("\\d+", "\\#");
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
                    Tag tagEnd = new Tag("<N+1>");
                    incrementCount(unigramCount, tagEnd);
                    incrementCount(bigramCount, tag, tagEnd);
                    incrementCount(trigramCount, tagPrevPrev, tag, tagEnd);
                }
            } finally {
                reader.close();
            }                
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void calculateNgrams() {
        for (String str1: new Tag().getTags()) {
            Tag tag1 = new Tag(str1);
            
            if (!unigramCount.containsKey(tag1)) {
                unigramCount.put(tag1, 0);
            }

            int nominator = unigramCount.get(tag1);
            int denominator = tokenCount;
            if (denominator != 0) {
                unigram.put(tag1, (double) nominator / denominator);
            } else {
                unigram.put(tag1, 0.0);
            }
        }


        for (String str1: new Tag().getTags()) {
            for (String str2: new Tag().getTags()) {
                Tag tag1 = new Tag(str1);
                Tag tag2 = new Tag(str2);

                if (!bigramCount.containsKey(tag1)) {
                    bigramCount.put(tag1, new HashMap<Tag, Integer>());
                }
                if (!bigramCount.get(tag1).containsKey(tag2)) {
                    bigramCount.get(tag1).put(tag2, 0);
                }
                if (!bigram.containsKey(tag1)) {
                    bigram.put(tag1, new HashMap<Tag, Double>());
                }

                int nominator = bigramCount.get(tag1).get(tag2);
                int denominator = unigramCount.get(tag1);
                if (denominator != 0) {
                    bigram.get(tag1).put(tag2, (double) nominator / denominator);
                } else {
                    bigram.get(tag1).put(tag2, 0.0);
                }
            }
        }

        for (String str1: new Tag().getTags()) {
            for (String str2: new Tag().getTags()) {
                for (String str3: new Tag().getTags()) {
                    Tag tag1 = new Tag(str1);
                    Tag tag2 = new Tag(str2);
                    Tag tag3 = new Tag(str3);

                    if (!trigramCount.containsKey(tag1)) {
                        trigramCount.put(tag1, new HashMap<Tag, HashMap<Tag, Integer>>());
                    }
                    if (!trigramCount.get(tag1).containsKey(tag2)) {
                        trigramCount.get(tag1).put(tag2, new HashMap<Tag, Integer>());
                    }
                    if (!trigramCount.get(tag1).get(tag2).containsKey(tag3)) {
                        trigramCount.get(tag1).get(tag2).put(tag3, 0);
                    }
                    if (!trigram.containsKey(tag1)) {
                        trigram.put(tag1, new HashMap<Tag, HashMap<Tag, Double>>());
                    }
                    if (!trigram.get(tag1).containsKey(tag2)) {
                        trigram.get(tag1).put(tag2, new HashMap<Tag, Double>());
                    }

                    int nominator = trigramCount.get(tag1).get(tag2).get(tag3);
                    int denominator = bigramCount.get(tag1).get(tag2);
                    if (denominator != 0) {
                        trigram.get(tag1).get(tag2).put(tag3, (double) nominator / denominator);
                    } else {
                        trigram.get(tag1).get(tag2).put(tag3, 0.0);
                    }
                }
            }
        }
    }

    private static void calculateLambdas() {
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

                    double val1;
                    if (tokenCount == 1)
                        val1 = 0;
                    else
                        val1 = (double) (unigramCount.get(tag3) - 1) / (tokenCount - 1);
                    
                    double val2;
                     if (unigramCount.get(tag2) == 1)
                        val2 = 0;
                    else
                        val2 = (double) (bigramCount.get(tag2).get(tag3) - 1) / (unigramCount.get(tag2) - 1);
                    
                    double val3;
                    if(bigramCount.get(tag1).get(tag2) == 1)
                        val3 = 0;
                    else
                        val3 = (double) (trigramCount.get(tag1).get(tag2).get(tag3) - 1) / (bigramCount.get(tag1).get(tag2) - 1);
                    
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

    private static void calculateSingeltonCount() {
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

    private static void calculateTypeandToken() {
        typeCount = 0;
        tokenCount = 0;
        for (Integer value: wordCount.values()) {
            typeCount += 1;
            tokenCount += value;
        }
    }

    private static void createModel() {
        calculateTypeandToken();
        calculateSingeltonCount();
        calculateNgrams();
        calculateLambdas();

        model = new Model(wordCount, unigramCount, lexicalCount, singeltonCount, unigram, bigram, trigram, lambda1, lambda2, lambda3, typeCount, tokenCount);
    }

    private static void saveModel(String[] args) {
        try {
            FileOutputStream fileStream = new FileOutputStream(args[2]);
            ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
            try {
                objectStream.writeObject(model); 
            } finally {
                if (objectStream != null) { 
                    objectStream.close();
                    fileStream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void test() {
        for (String str: new Tag().getTags()) {
            Tag tag = new Tag(str);
            truePositives.put(tag, 0);
            falsePositives.put(tag, 0);
            falseNegatives.put(tag, 0);
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(developmentFile));
            try {
                String line;
                int count = 0;
                while ((line = reader.readLine()) != null) {
                    System.out.println(++count);
                    String[] tuples = line.split(" ");
                    String[] words = new String[tuples.length];
                    Tag[] trueTags = new Tag[tuples.length];
                    for (int i = 0; i < tuples.length; i++) {
                        int split = tuples[i].lastIndexOf('/');
                        words[i] = tuples[i].substring(0, split);
                        trueTags[i] = new Tag(tuples[i].substring(split + 1));
                    }
                    Tag[] assignedTags = model.tag(words);

                    for (int i = 0; i < tuples.length; i++) {
                        if (trueTags[i].equals(assignedTags[i])) {
                            incrementCount(truePositives, trueTags[i]);
                        } else {
                            incrementCount(falseNegatives, trueTags[i]);
                            incrementCount(falsePositives, assignedTags[i]);
                        }
                    }
                }
            } finally {
                reader.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
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
}