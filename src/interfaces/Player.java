package interfaces;

import java.util.ArrayList;

import racko.Game;
import racko.Rack;

/**
 * All Racko players (AI or Human) need to implement this class;
 * We assume the players won't cheat (e.g. play when it is not their
 * turn or override class variables with reflection)
 */
public abstract class Player {
	public Game game;
	public Rack rack;
	public int score, wins;
	
	protected Player()
	{
		
	}
	
	/**
	 * Register a game to be played
	 * @param g a game to register
	 * @param r the player's rack
	 */
	public final void register(Game g, Rack r){
		game = g;
		rack = r;
		score = 0;
		wins = 0;
	}
	/**
	 * Notifies the player that their turn has arrived; the player must call
	 * Deck.draw() once, followed by Deck.discard() once (along with any
	 * calculations)
	 */
	public abstract void play();
	/**
	 * Notifies the player of the outcome for each round
	 * @param won whether this player won the round
	 * @param score their final score for the round
	 */
	public abstract void scoreRound(boolean won, int score);
	/**
	 * Notifies the player of the outcome for the game
	 * @param won whether this player won the game
	 * @param final their final score for the game
	 */
	public abstract void scoreGame(boolean won);
	
	public ArrayList<Integer> getPublicCards()
	{
		return rack.getVisibleCards();
	}
}
