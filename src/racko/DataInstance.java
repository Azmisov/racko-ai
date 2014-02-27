package racko;

public class DataInstance
{
	private int[] rack;
	double[] probabilityHigher;
	double[] probabilityLower;
	int drawDiscard;
	int output;
	
	public DataInstance()
	{
		rack = null;
		probabilityHigher = null;
		probabilityLower = null;
		drawDiscard = -1;
		output = -1;
	}
	
	public DataInstance(int[] currentRack, double[] pHigh, double[] pLow, int drawDiscard)
	{	
		setRack(currentRack);
		setProbabilityHigher(pHigh);
		setProbabilityLower(pLow);
		setDrawDiscard(drawDiscard);
		setOutput(-1);
	}
	
	public int[] getRack()
	{
		return rack;
	}
	
	public void setRack(int[] newRack)
	{
		rack = new int[newRack.length];
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
	
	public int getOutput()
	{
		return output;
	}
	
	public void setOutput(int newOutput)
	{
		output = newOutput;
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
