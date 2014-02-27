package client;

import java.util.ArrayList;

import interfaces.Player;
import racko.DataInstance;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{

	ArrayList<DataInstance> playHistory;
	
	public PlayerAI()
	{
		super();
		
		playHistory = new ArrayList<DataInstance>();
	}
	
	@Override
	public void play() {
		throw new UnsupportedOperationException("Not supported yet.");
		
		DataInstance thisMove = new DataInstance();
		
		addMoveToHistory(thisMove);
	}

	@Override
	public void scoreRound(boolean won, int score) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void scoreGame(boolean won) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	private void addMoveToHistory(DataInstance move)
	{
		playHistory.add(move);
	}
	
}
