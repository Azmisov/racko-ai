package NeuralNetworks;

import java.util.ArrayList;

/**
 * A generic multi-layered neural network, based on "perceptrons."
 * You can do nested networks, since this class also extends Node
 * TODO: nested networks is completely broken at this point...
 * @author isaac
 */
public class Network extends Node{
	//Input nodes for data features; 
	private ArrayList<ArrayList<Node>> layers;
	private int input_nodes, frozen;
	
	/**
	 * Builds a standard multi-layered network
	 * @param layers node counts for each layer
	 * @throws Exception if there are no layers or a layer has non-positive nodes
	 */
	public Network(int[] layers) throws Exception{
		super();
		
		if (layers == null || layers.length == 0)
			throw new Exception("Network must have at least one layer");
		
		this.layers = new ArrayList();
		
		//Create nodes for each layer
		input_nodes = layers[0];
		for (int i=0, count; i<layers.length; i++){
			ArrayList<Node> layer = new ArrayList();
			if ((count = layers[i]) <= 0)
				throw new Exception("Layers must have positive node count");
			for (int j=0; j<count; j++)
				layer.add(new Node());
			this.layers.add(layer);
		}
		
		//Create links
		for (int i=0; i<layers.length-1; i++){
			ArrayList<Node> prev = this.layers.get(i),
							next = this.layers.get(i+1);
			for (Node n: next){
				for (Node p: prev)
					p.addOutlink(n);
			}
		}
		
		//One bias input (for each layer) for all hidden/output nodes
		for (int i=1; i<layers.length; i++){
			Node bias = new Node();
			this.layers.get(i-1).add(bias);	
			for (Node link: this.layers.get(i))
				bias.addOutlink(link);
		}

		/*
		ArrayList<Node> last_layer = this.layers.get(layers.length-1);
		//Add bias for network node
		last_layer.add(new Node());
		//Final layer outputs to the "network node" (result node)
		for (Node last: last_layer)
			last.addOutlink(this);
		*/
		frozen = 0;
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
		//Create hidden layer
		ArrayList<Node> hidden = new ArrayList();
		for (int i=0; i<nodes; i++)
			hidden.add(new Node());
		//Remove old connections
		int layer_count = layers.size();
		ArrayList<Node> last = layers.get(layer_count-2), output = layers.get(layer_count-1);
		for (Node n: last){
			n.links_out.clear();
			n.weights.clear();
			n.weight_delta = 0;
			//Connect the new hidden layer
			for (Node h: hidden)
				n.addOutlink(h);
		}
		//Create bias for hidden layer (must come after connecting "last")
		hidden.add(new Node());
		for (Node n: output){
			n.links_in.clear();
			//Connect the new hidden layer
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
	 * @throws Exception if the data is ill-formed
	 */
	public void compute(double[] data) throws Exception{
		if (data.length != input_nodes){
			System.out.println(data.length+" does not match "+input_nodes);
			throw new Exception("Invalid data size");
		}
		
		//Set initial net values
		for (int i=0, li=layers.size(); i<li; i++){
			ArrayList<Node> layer = layers.get(i);
			for (int j=0, lj=layer.size(); j<lj; j++){
				Node temp = layer.get(j);
				//Input node
				if (i == 0 && j < data.length)
					temp.net = data[j];
				//No in-links = bias node
				else if (temp.links_in.isEmpty())
					temp.net = 1;
				//Default node, reset net value
				else temp.net = 0;
				//Reset training error
				temp.err = 0;
				temp.out = 0;
			}
		}
		//TODO: reset for nested networks
		/*
		this.net = 0;
		this.err = 0;
		this.out = 0;
		*/
		
		//Go through each layer, incrementally, and compute net values
		boolean first = true;
		for (ArrayList<Node> layer: layers){
			//Compute net values
			for (Node in: layer){
				//TODO, this breaks regular perceptrons
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
	public int getOutput(int target){
		int l = layers.size();
		ArrayList<Node> last = layers.get(l-1);
		//If only one output node, return binary answer (1/0)
		if (last.size() == 1)
			return last.get(0).out > 0 ? 1 : 0;
		//Otherwise, take the output with the higheset output
		else{
			double max_val = last.get(0).out;
			int max_idx = 0;
			for (int i=1, last_l=last.size(); i<last_l; i++){
				double temp = last.get(i).out;
				if (temp > max_val){
					max_val = temp;
					max_idx = i;
				}
			}
			return max_idx;
		}
	}
	
	/**
	 * Adjusts the network's weights to minimize some objective function;
	 * This uses the cached node output values from the previous call to
	 * "compute()"
	 * @param rate the learning rate
	 * @param target the target output value
	 * @return whether it got a valid answer
	 */
	public boolean trainPerceptron(double rate, double target){
		//Using Delta Rule: rate*(target - output)*node_output
		//TODO: what if we want continuous labels??
		double delta = rate * (target - (net > 0 ? 1 : 0));
		//No need to adjust weights if we got the answer right
		if (delta == 0)
			return true;
		//Otherwise, adjust weights proportional to net values
		for (ArrayList<Node> layer: layers){
			for (Node node: layer)
				 node.weights.set(0, node.weights.get(0)+delta*node.net);
		}
		return false;
	}
	/**
	 * Adjusts the network's weights, using the backpropagation rule
	 * @param rate the learning rate
	 * @param targets an array of target values, corresponding to the output nodes
	 * @throws Exception if the targets don't match the output nodes
	 */
	public void trainBackprop(double rate, double momentum, double[] targets) throws Exception{
		//TODO: what if we have nested networks?
		int layer_count = layers.size();
		ArrayList<Node> layer = layers.get(layer_count-1);
		if (targets.length != layer.size())
			throw new Exception("Target values don't match output nodes");
		//First, calculate error for output nodes
		//Err = [derivative of sigmoid]*[delta rule] OR output*(1-output)(target-output)
		for (int i=0, l=layer.size(); i<l; i++){
			Node temp = layer.get(i);
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
					double weight = node.weights.get(j),
							delta = rate*node.links_out.get(j).err*node.out;
					//Calculate momentum term
					if (Math.signum(delta) != Math.signum(node.weight_delta))
						node.weight_delta = delta;
					else node.weight_delta = node.weight_delta*momentum + delta;
					//Adjust weight
					weight += node.weight_delta;
					node.weights.set(j, weight);
				}
			}
		}
	}
	/**
	 * Train with backpropagation, using classification
	 * @param rate the learning rate
	 * @param target the target class
	 * @throws Exception 
	 */
	public void trainBackprop(double rate, double momentum, int target) throws Exception{
		int outputs = layers.get(layers.size()-1).size();
		double[] targets;
		targets = new double[outputs];
		if (outputs == 1)
			targets[0] = target;
		else
			targets[target] = 1;
		trainBackprop(rate, momentum, targets);
	}
	
	@Override
	public String toString(){
		int idx = 1;
		StringBuilder sb = new StringBuilder();
		/*
		for (ArrayList<Node> layer: layers){
			sb.append("Layer ").append(idx++).append(": ");
			for (Node n: layer)
				sb.append(n.weight).append("\t");
		}
		//*/
		return sb.toString();
	}
}
