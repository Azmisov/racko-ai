package racko;

import interfaces.DataInstance;
import java.util.HashMap;

/**
 * Contains a history of moves the computer made for drawing the draw pile/discard pile.
 * This essentially keeps track of moves and if they are successful or not.
 * @author Chris
 *
 */
public class MoveHistory
{
	private HashMap<DataInstance, Integer> moveHistory;
	public HashMap<DataInstance, Integer> tempMoveHistory;
	
	public MoveHistory()
	{
		moveHistory = new HashMap<DataInstance, Integer>();
		tempMoveHistory = new HashMap<DataInstance, Integer>();
	}
	
	public int tempSize()
	{
		return tempMoveHistory.size();
	}
	
	public void add(DataInstance instance)
	{
		if(moveHistory.containsKey(instance))
		{
			int value = moveHistory.get(instance);
			value++;
			moveHistory.put(instance, value);
		}
		else
		{
			moveHistory.put(instance, 1);
		}
	}
	
	public void addTemp(DataInstance instance)
	{
		if(tempMoveHistory.containsKey(instance))
		{
			int value = tempMoveHistory.get(instance);
			value++;
			tempMoveHistory.put(instance, value);
		}
		else
		{
			tempMoveHistory.put(instance, 1);
		}
	}
	
	
	
	
}
