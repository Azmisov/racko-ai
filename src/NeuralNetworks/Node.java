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
	protected double net, out, err, weight_delta;
	//Links
	protected ArrayList<Node> links_out, links_in;
	protected ArrayList<Double> weights;
	
	public Node(){
		links_out = new ArrayList();
		links_in = new ArrayList();
		weights = new ArrayList();
		weight_delta = 0;
	}
	
	public void addOutlink(Node n){
		links_out.add(n);
		n.links_in.add(this);
		double weight = ran.nextDouble()/ADJUST - 1/ADJUST/2;
		weights.add(weight);
	}
	
	public double getNetValue(){
		return net;
	}
}
