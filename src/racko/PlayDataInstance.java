package racko;

public class PlayDataInstance
{
	private int[] rack;
	private int card;
	private int output;
	
	public PlayDataInstance()
	{
		rack = null;
		card = -1;
		output = -1;
	}
	
	public PlayDataInstance(int[] currentRack, int currentCard)
	{
		setRack(currentRack);
		setCard(currentCard);
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
	
	public int getCard()
	{
		return card;
	}
	
	public void setCard(int newCard)
	{
		card = newCard;
	}
	
	public int getOutput()
	{
		return output;
	}
	
	public void setOutput(int newOutput)
	{
		output = newOutput;
	}
}
