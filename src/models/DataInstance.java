package models;

/**
 * Generic inputs instance for neural network training
 */
public class DataInstance {
	public double[] inputs;
	public double output;
	private int loc = 0;
	
	public DataInstance(int features){
		inputs = new double[features];
	}
	
	//Integer features
	public void addFeature(int[] vals, double normalize){
		for (int i=0; i<vals.length; i++)
			inputs[loc++] = vals[i] / normalize;
	}
	public void addFeature(int val, double normalize){
		inputs[loc++] = val / normalize;
	}
	
	//Double features
	public void addFeature(double[] vals, double normalize){
		if (normalize == 1){
			System.arraycopy(vals, 0, inputs, loc, vals.length);
			loc += vals.length;
		}
		else{
			for (int i=0; i<vals.length; i++)
				inputs[loc++] = vals[i] / normalize;
		}
	}
	public void addFeature(double val, double normalize){
		inputs[loc++] = val / normalize;
	}
	
	//Boolean features
	public void addFeature(boolean[] vals){
		for (int i=0; i<vals.length; i++)
			inputs[loc++] = vals[i] ? 1 : 0;
	}
	public void addFeature(boolean val){
		inputs[loc++] = val ? 1 : 0;
	}
	
	//Output value for different inputs types
	public void setOutput(int val, double normalize){
		output = val / normalize;
	}
	public void setOutput(double val, double normalize){
		output = val / normalize;
	}
	public void setOutput(boolean val){
		output = val ? 1 : 0;
	}
}
