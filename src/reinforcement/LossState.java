/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reinforcement;

public class LossState extends State {
	public LossState(boolean b){super(b);};
	
	@Override
	public double stateScore()
	{
		return -10000;
	}
	
}
