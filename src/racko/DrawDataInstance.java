package racko;

import interfaces.DataInstance;

/**
 * Represents a data instance used by the machine learner to learn whether to draw from the discard pile or from the draw pile.
 * @author Chris
 *
 */
public class DrawDataInstance implements DataInstance
{
	private double[] rack;
	private double[] probabilityHigher;
	private double[] probabilityLower;
	private int drawDiscard;
	private boolean output;
	
	public DrawDataInstance()
	{
		rack = null;
		probabilityHigher = null;
		probabilityLower = null;
		drawDiscard = -1;
		output = false;
	}
	
	public DrawDataInstance(int[] currentRack, double[] pHigh, double[] pLow, int drawDiscard)
	{	
		setRack(currentRack);
		setProbabilityHigher(pHigh);
		setProbabilityLower(pLow);
		setDrawDiscard(drawDiscard);
		setOutput(false);
		
		
	}
	
	public double[] getRack()
	{
		return rack;
	}
	
	public void setRack(int[] newRack)
	{
		rack = new double[newRack.length];
		for(int i = 0; i < newRack.length; i++)
		{
			rack[i] = newRack[i];
		}
	}
	
	public double[] getProbabilityHigher()
	{
		return probabilityHigher;
	}
	
	public void setProbabilityHigher(double[] pHigh)
	{
		probabilityHigher = new double[pHigh.length];
		for(int i = 0; i < pHigh.length; i++)
		{
			probabilityHigher[i] = pHigh[i];
		}
	}
	
	public double[] getProbabilityLower()
	{
		return probabilityLower;
	}
	
	public void setProbabilityLower(double[] pLow)
	{
		probabilityLower = new double[pLow.length];
		for(int i = 0; i < pLow.length; i++)
		{
			probabilityLower[i] = pLow[i];
		}
	}
	
	public int getDrawDiscard()
	{
		return drawDiscard;
	}
	
	public void setDrawDiscard(int newDrawDiscard)
	{
		drawDiscard = newDrawDiscard;
	}
	
	/**
	 * Gets the classification of the data instance.
	 * True = Draw from draw pile. False = Draw from the discard pile.
	 * @return
	 */
	public boolean getOutput()
	{
		return output;
	}
	
	public void setOutput(boolean newOutput)
	{
		output = newOutput;
	}
	
	public double[] getInputs()
	{
		double[] rval = new double[rack.length+probabilityHigher.length+probabilityLower.length+1];
		
		System.arraycopy(rack, 0, rval, 0, rack.length);
		System.arraycopy(probabilityHigher, 0, rval, rack.length, probabilityHigher.length);
		System.arraycopy(probabilityLower, 0, rval, rack.length+probabilityHigher.length, probabilityLower.length);
		rval[rval.length-1] = drawDiscard; 
		
		return rval;
	}
	
	public String toString()
	{
		String rval = "";
		
		for(int i = 0; i < rack.length; i++)
		{
			rval += rack[i];
			rval += ",";
		}
		
		for(int i = 0; i < probabilityHigher.length; i++)
		{
			rval += probabilityHigher[i];
			rval += ",";
		}
		
		for(int i = 0; i < probabilityLower.length; i++)
		{
			rval += probabilityLower[i];
			rval += ",";
		}
		
		rval += drawDiscard;
		rval += ",";
		rval += output;
		
		return rval;
	}
}
