/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reinforcement;
import java.io.*;

public class State {
	Action[] actions;
	boolean drawState;
	
	public State(boolean draw)
	{
		drawState = draw;
		if (draw)
			actions = new Action[2];
		else
			actions = new Action[6];
		
		for (int i=0; i < actions.length; i++)
			actions[i] = new Action();
	}
	public State(boolean draw, BufferedReader buff)
	{
		drawState = draw;
		if (draw)
			actions = new Action[2];
		else
			actions = new Action[6];
		
		for (int i=0; i < actions.length; i++)
			actions[i] = new Action(buff);
	}
	public double stateScore()
	{
		double maxScore = -1 * Double.MAX_VALUE;
		for (Action a : actions)
			if (a.getReward() > maxScore)
				maxScore = a.getReward();
		return maxScore;
	}
	public int getLeastVisited()
	{
		int index = 0;
		int minVisits = Integer.MAX_VALUE;
		for (int i=0; i < actions.length; i++)
		{
			if (actions[i].getVisits() < minVisits)
			{
				minVisits = actions[i].getVisits();
				index = i;
			}
		}
		return index;
	}
	public void updateReward(State next, int actionIndex)
	{
		actions[actionIndex].updateReward(next);
	}
	public int getBestReward()
	{
		int index = 0;
		double maxReward = -Integer.MAX_VALUE;
		for (int i=0; i < actions.length; i++)
		{
			if (actions[i].getReward() > maxReward)
			{
				maxReward = actions[i].getReward();
				index = i;
			}
		}
		return index;
	}
	public boolean allVisitedMore(int comp)
	{
		for (Action a : actions)
			if (a.getVisits() < comp)
				return false;
		return true;
	}
	public String saveString()
	{
		String toReturn = "";
		for (Action a: actions)
			toReturn = toReturn + a.saveString();
		return toReturn;
	}
	
}
