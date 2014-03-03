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
	
	@Override
<<<<<<< 16426c1ff2ea805a8700b834d1a77e47e61b636f
	public int play() {
		//throw new UnsupportedOperationException("Not supported yet.");
		
		boolean drawFromDiscard = rand.nextBoolean();
=======
	public void play() {
		boolean drawFromDiscard = decideDraw();//
>>>>>>> ccc2c7878dd13ace2aafd10abfd6874b3e62169d
		
		int card = game.deck.draw(drawFromDiscard);
		
		//System.out.println(playerNumber + ": drew card number: " + card);
		
<<<<<<< 16426c1ff2ea805a8700b834d1a77e47e61b636f
		//DataInstance thisMove = new DataInstance(rack.getCards(), );
		
		int slot = rand.nextInt(game.rack_size+1) - 1;
=======
		int slot = decidePlay(rack, card);
>>>>>>> ccc2c7878dd13ace2aafd10abfd6874b3e62169d
		
		int discard = slot == -1 ? card : rack.swap(card, slot, false);
		
		//System.out.println(playerNumber + ": rack: " + rack.toString());
		
<<<<<<< 16426c1ff2ea805a8700b834d1a77e47e61b636f
		//addMoveToHistory(thisMove);
		
		//System.out.println(playerNumber + ": discarded card number: " + discard);
=======
		System.out.println(playerNumber + ": discarded card number: " + discard);
>>>>>>> ccc2c7878dd13ace2aafd10abfd6874b3e62169d
		
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
	
<<<<<<< 16426c1ff2ea805a8700b834d1a77e47e61b636f
	
	
	private void addMoveToHistory(DataInstance move){
=======
	private void addMoveToHistory(DrawDataInstance move)
	{
>>>>>>> ccc2c7878dd13ace2aafd10abfd6874b3e62169d
		playHistory.add(move);
	}
	
}
