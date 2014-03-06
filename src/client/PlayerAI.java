package client;

import java.util.ArrayList;
import java.util.Random;

import interfaces.Player;
import racko.DrawDataInstance;
import racko.Deck;
import racko.Game;
import racko.MoveHistory;
import racko.PlayDataInstance;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{

	private MoveHistory drawHistory;
	private MoveHistory playHistory;
	
	Random rand;
	
	public PlayerAI(){
		super();
		
		drawHistory = new MoveHistory();
		playHistory = new MoveHistory();
		rand = new Random();
	}
	
	public int play() {
		boolean drawFromDiscard = decideDraw();
		
		int card = game.deck.draw(drawFromDiscard);
		
		System.out.println(playerNumber + ": drew card number: " + card);
		
		int slot = decidePlay(rack, card);
		
		int discard = slot == -1 ? card : rack.swap(card, slot, drawFromDiscard);
		
		System.out.println(playerNumber + ": rack: " + rack.toString());
		System.out.println(playerNumber + ": discarded card number: " + discard);
		
		return discard;
	}

	private boolean decideDraw(){
		boolean rval = false;
		
		int topOfDiscard = game.deck.peek(true);
		
		rval = rand.nextBoolean();
		
		addDrawToHistory(rval, topOfDiscard);
		
		return rval;
	}
	
	private int decidePlay(Rack rack, int card) {
		int rval = -1;
		
		rval = rand.nextInt(Rack.rack_size+1) - 1;
		
		addPlayToHistory(rval);
		
		return rval;
	}

	@Override
	public void scoreRound(boolean won, int score) {
		System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" ROUND, score = "+score);
	}

	@Override
	public void scoreGame(boolean won) {
		System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" GAME, score = "+score);
		
		saveMoveHistory(won);
	}
	
	private void addDrawToHistory(boolean card, int topOfDiscard)
	{
		//create a DrawDataInstance and fill it with the information
		int [] currentRack = rack.getCards();
		double [] pHigh = new double[currentRack.length];
		double [] pLow = new double[currentRack.length];
		for (int i=0; i < currentRack.length; i++)
		{
			pHigh[i] = game.deck.getProbability(currentRack[i], rack, true, 0, true);
			pLow[i] = game.deck.getProbability(currentRack[i], rack, true, 0, false);
		}
		int discard = game.deck.peek(true);
		DrawDataInstance DDI = new DrawDataInstance(currentRack, pHigh, pLow, discard);
		
		drawHistory.addTemp(DDI);
	}
	
	private void addPlayToHistory(int slot)
	{
		
		
		playHistory.addTemp(new PlayDataInstance(rack.getCards(), slot));
	}
	
	private void saveMoveHistory(boolean won)
	{
		
	}
	
	
}
