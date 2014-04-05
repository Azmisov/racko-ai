/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reinforcement;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Action {
	private double reward;
	private int visits;
	
	//For value update equation. Can be set to any value between 0 and 1.
	//Lower values cause it to value closer rewards more than far off rewards.
	private final double gamma = 0.25;
	
	public Action()
	{
		reward = 0;
		visits = 0;
	}
	public Action(BufferedReader buff)
	{
		visits = 0;
		try {
			reward = Double.parseDouble(buff.readLine());
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	public double getReward()
	{
		return reward;
	}
	public int getVisits()
	{
		return visits;
	}
	public void updateReward(State s)
	{
		double outputReward = s.stateScore();
		if (outputReward != 0)
		{
			visits++;
			
			double alpha = (double)(1/(double)visits);
			double secondpart = alpha * gamma * outputReward;
			double firstpart = (1-alpha) * reward;
			reward = firstpart + secondpart;
			
		}
	}
	public String saveString()
	{
		return "" + reward + "\n";
	}
	
}
