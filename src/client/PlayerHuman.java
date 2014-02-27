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
	public void play() {
		//throw new UnsupportedOperationException("Not supported yet.");
		System.out.println("Play");
	}

	@Override
	public void scoreRound(boolean won, int score) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void scoreGame(boolean won) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
