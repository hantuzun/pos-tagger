import java.util.Arrays;
import java.util.HashSet;

public final class Tag {
    private static final HashSet<String> tags = new HashSet<String>(Arrays.asList("<t_(-1)>", "<t_(0)>", "CC", 
        "CD", "DT", "EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNS", "NNP", "NNPS", "PDT", "POS",
        "PRP", "PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", 
        "WDT", "WP", "WP$", "WRB", "$", "#", "``", "''", "-LRB-", "-RRB-", ",", ".", ":", "<t_(N+1)>"));

    public static final int count = tags.size();

    private String tag;
    public Tag(String tag) {
        if (tags.contains(tag))
            this.tag = tag;
        else
            throw new IllegalArgumentException("\'" + tag + "\' is not a valid POS tag.");
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
        else if (this.tag != ((Tag) o).tag)
            return false;
        else
            return true;
    }

    @Override
    public String toString() {
        return tag;
    }
}
