/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reinforcement;

public class WinState extends State {
	public WinState(boolean b){super(b);};
	
	@Override
	public double stateScore()
	{
		return 10000;
	}
	
}
