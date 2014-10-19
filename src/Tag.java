import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;

public final class Tag implements Serializable{
        private static final long serialVersionUID = 4901234129073L;

    private static final HashSet<String> tags = new HashSet<String>(Arrays.asList(
        "CC", "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS",
        "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", 
        "WP", "WP$", "WRB", "$", "#", "``", "''", "-LRB-", "-RRB-", ",", ".", ":", "<-1>", "<0>", "<N+1>"));

    public static final int count = tags.size();

    private String tag;

    public Tag() {}

    public Tag(String tag) {
        if (tags.contains(tag))
            this.tag = tag;
        else
            throw new IllegalArgumentException("\'" + tag + "\' is not a valid POS tag.");
    }

    public HashSet<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return tag;
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) 
            return false;
        else if (!(o instanceof Tag))
            return false;
        else if (!this.toString().equals(((Tag) o).toString()))
            return false;
        else
            return true;
    }


    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
    }

    private void writeObject(ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
    }
}
