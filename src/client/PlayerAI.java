package client;

import java.util.ArrayList;
import java.util.Random;

import interfaces.Player;
import racko.DrawDataInstance;
import racko.Deck;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{

	ArrayList<DrawDataInstance> playHistory;
	Random rand;
	
	public PlayerAI(){
		super();
		
		playHistory = new ArrayList<DrawDataInstance>();
		rand = new Random();
	}
	
	public int play() {
		boolean drawFromDiscard = decideDraw();//
		
		int card = game.deck.draw(drawFromDiscard);
		
		System.out.println(playerNumber + ": drew card number: " + card);
		int slot = decidePlay(rack, card);
		
		int discard = slot == -1 ? card : rack.swap(card, slot, false);
		
		System.out.println(playerNumber + ": rack: " + rack.toString());
		System.out.println(playerNumber + ": discarded card number: " + discard);
		
		return discard;
	}

	private int decidePlay(Rack rack, int card) {
		int rval = -1;
		
		rval = rand.nextInt(Rack.rack_size+1) - 1;
		
		return rval;
	}

	private boolean decideDraw()
	{
		boolean rval = false;
		
		rval = rand.nextBoolean();
		
		return rval;
	}

	@Override
	public void scoreRound(boolean won, int score) {
		System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" ROUND, score = "+score);
	}

	@Override
	public void scoreGame(boolean won) {
		System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" GAME, score = "+score);
	}
	
	private void addMoveToHistory(DrawDataInstance move)
	{
		playHistory.add(move);
	}
	
}
