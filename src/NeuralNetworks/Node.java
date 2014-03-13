package NeuralNetworks;

import java.util.ArrayList;
import java.util.Random;

/**
 * Network neurons
 * @author isaac
 */
public class Node{
	private static final double ADJUST = 10;
	private static final Random ran = new Random();
	protected double net, out, err;
	protected final boolean is_bias;
	//Links
	protected final ArrayList<Node> links_out;
	protected final ArrayList<Double> weights;
	
	public Node(boolean bias){
		links_out = new ArrayList();
		weights = new ArrayList();
		is_bias = bias;
	}
	
	public void addOutlink(Node n){
		//Ignore bias weights as outputs
		if (!n.is_bias){
			links_out.add(n);
			double weight = ran.nextDouble()/ADJUST - 1/ADJUST/2;
			weights.add(weight);
		}
	}
	
	public double getNetValue(){
		return net;
	}
}
