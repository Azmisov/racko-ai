package client;

import java.util.ArrayList;
import java.util.Random;

import interfaces.Player;
import racko.DataInstance;
import racko.Deck;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{

	ArrayList<DataInstance> playHistory;
	Random rand;
	
	public PlayerAI(){
		super();
		
		playHistory = new ArrayList<DataInstance>();
		rand = new Random();
	}
	
	@Override
	public void play() {
		//throw new UnsupportedOperationException("Not supported yet.");
		
		boolean drawFromDiscard = rand.nextBoolean();
		
		int card = game.deck.draw(drawFromDiscard);
		
		System.out.println(playerNumber + ": drew card number: " + card);
		
		//DataInstance thisMove = new DataInstance(rack.getCards(), );
		
		int slot = rand.nextInt(Rack.rack_size+1) - 1;
		
		int discard = slot == -1 ? card : rack.swap(card, slot, false);
		
		System.out.println(playerNumber + ": rack: " + rack.toString());
		
		//addMoveToHistory(thisMove);
		
		System.out.println(playerNumber + ": discarded card number: " + discard);
		
		game.deck.discard(discard);
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
