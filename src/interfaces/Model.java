package interfaces;

import racko.Game;
import racko.Rack;

/**
 * A learning model that decides how to play
 * @author isaac
 */
public abstract class Model {
	protected Game game;
	protected Rack rack;
	
	/**
	 * Get this model's name
	 * @return 
	 */
	public String getName(){
		return "Computer";
	}
	/**
	 * Register this model with a particular game
	 * @param g game to use
	 * @param r rack to use
	 * @return true, if registration is successful
	 */
	public boolean register(Game g, Rack r){
		game = g;
		rack = r;
		return true;
	}
	/**
	 * Decide whether to draw form discard pile
	 * @param turn what turn is this in the round?
	 * @return true, if we should draw from discard
	 */
	public abstract boolean decideDraw(int turn);
	/**
	 * Decide where to place drawn card
	 * @param turn what turn is this in the round?
	 * @param drawn card that was drawn
	 * @param fromDiscard was it drawn from the discard pile?
	 * @return location to place card; -1 indicates it should be discarded 
	 */
	public abstract int decidePlay(int turn, int drawn, boolean fromDiscard);
	
	/**
	 * Called at the start of each round
	 */
	public void beginRound(){}
	/**
	 * Notifies the player of the outcome for each round
	 * @param won whether this player won the round
	 * @param score their final score for the round
	 */
	public void scoreRound(boolean won, int score){}
	/**
	 * Notifies model that an epoch has been reached
	 * @param p the player using this model
	 */
	public void epoch(Player p){}
}
