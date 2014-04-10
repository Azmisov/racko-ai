package interfaces;

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
	public static int playerCount = 0;
	public final int playerNumber;
	public final String name;
	public int
		score,	//score for this particular game
		wins;	//wins (rounds) for this particular game
	//Overall statistics; these are not reset after each game
	public int
		STAT_badmoves,	//number of bad moves (for a ML model, this is # of random moves made)
		STAT_allmoves,	//total number of moves
		STAT_wins,		//how many wins does this player have
		STAT_score,		//cummulative score over all rounds
		STAT_rounds;	//how many rounds do these stats represent?
	//Epoch statistics (for stopping criteria and such)
	public double
		EPOCH_badmoves,	//percentage of bad moves in last epoch
		EPOCH_allmoves,	//average moves per round in last epoch
		EPOCH_wins;		//percentage of wins in last epoch
	//Model statistics, same as epoch statistics, except "averaged"
	//Creep indicates how much current model contributes to model statistics
	public static double modelCreep = .04;
	private boolean first_epoch = true;
	public double
		MODEL_badmoves,
		MODEL_allmoves,
		MODEL_wins;
	
	protected Player(){
		this("Player");
	}
	protected Player(String name){
		this.name = name;
		playerNumber = ++playerCount;
	}
	
	/**
	 * Register a game to be played
	 * If you override this, you must call super.register!!!
	 * Otherwise, we can't guarantee this will work
	 * @param g a game to register
	 * @param r the player's rack
	 * @return true, if successfully registered
	 */
	public boolean register(Game g, Rack r){
		game = g;
		rack = r;
		score = 0;
		wins = 0;
		resetStats();
		return true;
	}
	/**
	 * Notifies the player that their turn has arrived; the player must call
	 * Deck.draw() once (along with any calculations); return the card to discard
	 * @return the card to discard
	 */
	public abstract int play();
	/**
	 * Notifies the player of the outcome for each round
	 * @param won whether this player won the round
	 * @param score their final score for the round
	 */
	public void scoreRound(boolean won, int score){}
	/**
	 * Notifies the player of the outcome for the game
	 * @param won whether this player won the game
	 * @param score their final score for the game
	 */
	public void scoreGame(boolean won, int score){}
	
	public void beginRound(){}
	public void beginGame(){}
	
	//STATISTICS
	/**
	 * Denote the end of an epoch; Can be used for
	 * stopping criteria, deep learning, etc.; If this method is
	 * overridden, super.epoch() must be called
	 */
	public void epoch(){
		EPOCH_allmoves = STAT_allmoves / (double) STAT_rounds;
		EPOCH_badmoves = STAT_badmoves / (double) STAT_allmoves;
		EPOCH_wins = STAT_wins / (double) STAT_rounds;
		resetStats();
		
		//Running average of this model's stats
		if (first_epoch){
			first_epoch = false;
			MODEL_badmoves = EPOCH_badmoves;
			MODEL_allmoves = EPOCH_allmoves;
			MODEL_wins = EPOCH_wins;
		}
		else{
			double inv = 1-modelCreep;
			MODEL_badmoves = inv*MODEL_badmoves + modelCreep*EPOCH_badmoves;
			MODEL_allmoves = inv*MODEL_allmoves + modelCreep*EPOCH_allmoves;
			MODEL_wins = inv*MODEL_wins + modelCreep*EPOCH_wins;
		}
	}
	/**
	 * Indicates that the current state of the Player is
	 * a new learning model (e.g. after adding a new deep learning layer)
	 */
	public void resetModel(){
		first_epoch = true;
	}
	/**
	 * Resets statistics back to zero
	 */
	private void resetStats(){
		STAT_allmoves = 0;
		STAT_badmoves = 0;
		STAT_rounds = 0;
		STAT_wins = 0;
		STAT_score = 0;
	}
}
