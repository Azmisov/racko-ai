package NeuralNetworks;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.Scanner;

/**
 * A generic multi-layered neural network
 * @author isaac
 */
public class Network{
	//Input nodes for data features; 
	public final ArrayList<Node[]> layers = new ArrayList();
	private int input_nodes;
	private int frozen;
	
	/**
	 * Builds a standard multi-layered network
	 * @param layers node counts for each layer
	 */
	public Network(int[] layers){
		createNetwork(layers);
		//No frozen layers to start out with (deep learning)
		frozen = 0;
	}
	/**
	 * Creates a network by importing predefined weights
	 * @param filename the exported network
	 * @throws Exception if there was an error loading the file
	 */
	public Network(String filename) throws Exception{
		try (FileReader x = new FileReader(filename)){
			Scanner s = new Scanner(x);
			String splitter = "(\r?\n|\t)";
			s.useDelimiter(splitter);
			
			//Frozen layers
			frozen = s.nextInt();
			s.nextLine();
			
			//Layer sizes
			String line2 = s.nextLine();
			String[] line2_vals = line2.split(splitter);
			int[] f_layers = new int[line2_vals.length];
			for (int i=0; i<f_layers.length; i++)
				f_layers[i] = Integer.parseInt(line2_vals[i]);
			createNetwork(f_layers);
			
			//Network weights
			for (Node[] layer: layers){
				for (Node n: layer){
					ListIterator<Double> iter = n.weights.listIterator();
					while (iter.hasNext()){
						iter.next();
						iter.set(s.nextDouble());
					}
					s.nextLine();
				}
			}
		}
	}
	private void createNetwork(int[] layers){
		//Network must have at least one layer
		assert(layers != null && layers.length != 0);
		input_nodes = layers[0];
		
		//Create nodes for each layer
		for (int i=0, count; i<layers.length; i++){
			count = layers[i];
			//Layers must have positive node count
			assert(count > 0);
			boolean hidden = i != layers.length-1;
			//Create the actual nodes
			Node[] layer = new Node[count+(hidden ? 1 : 0)];
			for (int j=0; j<count; j++)
				layer[j] = new Node(false);
			//Extra node for bias
			if (hidden)
				layer[count] = new Node(true);
			this.layers.add(layer);
		}
		
		//Create links
		for (int i=0; i<layers.length-1; i++){
			Node[] prev = this.layers.get(i),
					next = this.layers.get(i+1);
			for (Node n: next){
				for (Node p: prev)
					p.addOutlink(n);
			}
		}
	}
	
	/**
	 * Get how many input nodes are in the network
	 * @return number of input nodes
	 */
	public int inputNodes(){
		return input_nodes;
	}
	/**
	 * How many output nodes are in the network
	 * @return 
	 */
	public int outputNodes(){
		return layers.get(layers.size()-1).length;
	}
	/**
	 * How many hidden layers in the network
	 * @return 
	 */
	public int hiddenLayers(){
		return layers.size()-2;
	}
	/**
	 * Freezes a layer so weights are not adjusted; use for deep learning
	 * @param layer the hidden layer to freeze (all layers before this one
	 * are automatically frozen as well); starts with 1, ends with |layers|-3;
	 * use 0 to unfreeze all layers; need at least 1 hidden layer unfrozen
	 */
	public void freeze(int layer){
		assert(layer >= 0 && layer <= layers.size()-3);
		frozen = layer;
	}
	/**
	 * Inserts another hidden layer
	 * @param nodes 
	 */
	public void addHiddenLayer(int nodes){
		int layer_count = layers.size();
		Node[] last = layers.get(layer_count-2),
				output = layers.get(layer_count-1);
		//Create hidden layer
		Node[] hidden = new Node[nodes+1];
		for (int i=0; i<nodes; i++)
			hidden[i] = new Node(false);
		//Bias weight
		hidden[nodes] = new Node(true);
		
		//Connect last (old last hidden layer) to the new hidden layer
		for (Node n: last){
			n.links_out.clear();
			n.weights.clear();
			for (Node h: hidden)
				n.addOutlink(h);
		}
		//Connect the new hidden layer to the output layer
		for (Node n: output){
			for (Node h: hidden)
				h.addOutlink(n);
		}
		//Add the hidden layer to layers
		layers.add(layer_count-1, hidden);
	}
	
	/**
	 * Computes the output of this neural network. The output for each
	 * node is cached, for use with the "train()" method
	 * TODO: make this work for cyclic graphs??
	 * @param data the input data
	 */
	public void compute(double[] data){
		assert(data.length == input_nodes);
		
		//Set initial net values
		boolean first = true;
		for (Node[] layer: layers){
			for (int j=0; j<layer.length; j++){
				Node temp = layer[j];
				//Input node
				if (first && j < data.length)
					temp.net = data[j];
				//Bias node
				else if (temp.is_bias)
					temp.net = 1;
				//Default node, reset net value
				else temp.net = 0;
				//Reset training error
				temp.err = 0;
				temp.out = 0;
			}
			first = false;
		}
		
		//Go through each layer, incrementally, and compute net values
		first = true;
		for (Node[] layer: layers){
			//Compute net values
			for (Node in: layer){
				//Compute activation function (sigmoid)
				in.out = first ? in.net : 1/(1+Math.exp(-in.net));
				for (int i=0, l=in.links_out.size(); i<l; i++)
					in.links_out.get(i).net += in.out*in.weights.get(i);
			}
			first = false;
		}
	}
	/**
	 * Gets the output of the network; probably only works for backprop
	 * @return the output node with greatest net value
	 */
	public int getOutput(){
		int l = layers.size();
		Node[] last = layers.get(l-1);
		//If only one output node, return binary answer (1/0)
		if (last.length == 1)
			return last[0].out > 0 ? 1 : 0;
		//Otherwise, take the output with the higheset output
		else{
			double max_val = last[0].out;
			int max_idx = 0;
			for (int i=1; i<last.length; i++){
				if (last[i].out > max_val){
					max_val = last[i].out;
					max_idx = i;
				}
			}
			return max_idx;
		}
	}
	/**
	 * Get the output of a specific output node (zero indexed)
	 * @param node the output node index
	 * @return the node's output value
	 */
	public double getOutput(int node){
		Node[] last = layers.get(layers.size()-1);
		return last[node].out;
	}

	/**
	 * Adjusts the network's weights, using the backpropagation rule
	 * @param rate the learning rate
	 * @param targets an array of target values, corresponding to the output nodes
	 */
	public void trainBackprop(double rate, double[] targets){		
		//TODO: what if we have nested networks?
		int layer_count = layers.size();
		Node[] layer = layers.get(layer_count-1);
		
		assert(targets.length == layer.length);
		//First, calculate error for output nodes
		//Err = [derivative of sigmoid]*[delta rule] OR output*(1-output)(target-output)
		for (int i=0; i<layer.length; i++){
			Node temp = layer[i];
			temp.err = temp.out*(1-temp.out)*(targets[i]-temp.out);
		}
		//Now, we propagate the error back through the network
		//Err = [derivative of sigmoid]*[weighted sum of outlinks] OR output*(1-output)SUM(weight*error)
		for (int i=layer_count-2; i>frozen; i--){
			for (Node node: layers.get(i)){
				//Sum of outlinks
				for (int j=0, l=node.links_out.size(); j<l; j++)
					node.err += node.links_out.get(j).err*node.weights.get(j);
				//Multiply derivative
				node.err *= node.out*(1-node.out);
			}
		}
		//Execute weight change
		//Delta = [learning rate]*[error of output node]*[output of current node]
		for (int i=frozen; i<layer_count-1; i++){
			for (Node node: layers.get(i)){
				for (int j=0, l=node.links_out.size(); j<l; j++){
					//Old weight + Delta
					double weight = node.weights.get(j) + rate*node.links_out.get(j).err*node.out;
					node.weights.set(j, weight);
				}
			}
		}
	}
	/**
	 * Train with backpropagation, using classification
	 * @param rate the learning rate
	 * @param target the target class 
	 */
	public void trainBackprop(double rate, int target){
		int outputs = layers.get(layers.size()-1).length;
		double[] targets;
		targets = new double[outputs];
		if (outputs == 1)
			targets[0] = target == 1 ? 1 : -1;
		else{
			Arrays.fill(targets, -1);
			targets[target] = 1;
		}
		trainBackprop(rate, targets);
	}
	
	/**
	 * Export network to file for loading later
	 * @param filename name of file
	 * @return success, if returns true
	 */
	public boolean export(String filename){
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(frozen).append('\n');
			int i = layers.size();
			for (Node[] layer: layers){
				//Account for bias weights
				int layer_size = layer.length;
				if (--i != 0) layer_size--;
				sb.append(layer_size).append('\t');
			}
			sb.append('\n');
			for (Node[] layer: layers){
				for (Node n: layer){
					for (Double weight: n.weights)
						sb.append(weight).append('\t');
					sb.append('\n');
				}
			}
			try (FileWriter x = new FileWriter(new File(filename))) {
				x.write(sb.toString());
			}
			return true;
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
			return false;
		}
	}
}
