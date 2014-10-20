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
		try {
            BufferedReader reader = new BufferedReader(new FileReader(testFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
            try {
            	String line;
                while ((line = reader.readLine()) != null) {
                	String[] words = line.split(" ");
                	Tag[] tags = model.tag(words);

                	writer.write(words[0] + "/" + tags[0]);
                	for (int i = 1; i < words.length; i++) {
                		writer.write(" " + words[i] + "/" + tags[i]);
                	}
                	writer.newLine();
                }
            } finally {
            	reader.close();
            	writer.flush();
            	writer.close();
            }
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
}