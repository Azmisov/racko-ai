package racko;

/**
 * All Racko players (AI or Human) need to implement this interface;
 * We assume the players won't cheat (e.g. play when it is not their
 * turn or override class variables with reflection)
 */
public interface Player {
	/**
	 * Register a game to be played
	 * @param g a game to register
	 * @param r the player's rack
	 */
	public void register(Game g, Rack r);
	/**
	 * Notifies the player that their turn has arrived; the player must call
	 * Game.draw() once, followed by Game.discard() once (along with any
	 * calculations)
	 */
	public void play();
	/**
	 * Notifies the player of the outcome for each round
	 * @param won whether this player won the round
	 * @param score their final score for the round
	 */
	public void scoreRound(boolean won, int score);
	/**
	 * Notifies the player of the outcome for the game
	 * @param won whether this player won the game
	 * @param final their final score for the game
	 */
	public void scoreGame(boolean won, int score);
}
