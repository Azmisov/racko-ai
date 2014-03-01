package client;

import interfaces.Player;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as a human
 */
public class PlayerHuman extends Player{

	public PlayerHuman()
	{
		super();
	}
	
	@Override
	public int play() {
		System.out.println("Play");
		return 0;
	}	
}
