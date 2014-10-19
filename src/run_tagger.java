import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;

public class run_tagger {
	private static Model model;
    private static File modelFile;
    private static File testFile;
    private static File outFile;

	/**
	 * run_tagger.java
	 * usage: run_tagger <sents.test> <model_file> <sents.out>
	 */
	public static void main(String[] args) {
		validateArguments(args);
		loadModel(args);
		test();
        
        String[] words = "For six years , T. Marshall Hahn Jr. has made corporate acquisitions in the George Bush mode : kind and gentle .".split(" ");
        Tag[] tags = model.tag(words);
        
        System.out.println();
        for (Tag tag: tags) 
            System.out.print(tag + " ");
        System.out.println();
        System.out.println("IN CD NNS , NNP NNP NNP NNP VBZ VBN JJ NNS IN DT NNP NNP NN : JJ CC JJ . ");

        System.exit(0);
	}

	public static void validateArguments(String[] args) {
		if (args.length >= 3) {
			testFile = new File(args[0]);
			modelFile = new File(args[1]);
			outFile = new File(args[2]);
		} else {
            System.err.println("usage: run_tagger <sents.test> <model_file> <sents.out>");
            System.exit(1);
		}
	}

	public static void loadModel(String[] args) {
		try {
			FileInputStream fileStream = new FileInputStream(modelFile);
			ObjectInputStream objectStream = new ObjectInputStream(fileStream);
			try {
				model = (Model) objectStream.readObject();		
			} finally {
				objectStream.close();
				fileStream.close();
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static void test() {
	}
}