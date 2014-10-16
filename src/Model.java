import java.util.HashMap;
import java.io.Serializable;

public class Model implements Serializable {
	public Model(HashMap<Tag, HashMap<Tag, HashMap<Tag, Double>>> transitionProbability, HashMap<Tag, HashMap<String, Double>> emissionProbability) {}	
}