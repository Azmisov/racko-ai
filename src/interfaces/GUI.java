package interfaces;

import racko.Rack;

/**
 * Notifies the GUI of game changes
 */
public interface GUI {
	/**
	 * Notify GUI that a player's turn has come
	 * @param p the player
	 * @param player_index the player's number
	 * @param r the player's rack
	 */
	public void turn(Player p, int player_index, Rack r);
	/**
	 * Notifies GUI that a card has been drawn
	 * @param card the card that was drawn
	 * @param fromDiscard was this taken from the discard pile?
	 */
	public void draw(int card, boolean fromDiscard);
	/**
	 * Notifies GUI that a card was discarded
	 * @param card the card that was discarded
	 */
	public void discard(int card);
	/**
	 * Notifies GUI of the outcome of a round
	 */
	public void scoreRound();
	/**
	 * Notifies GUI of the outcome of a game
	 */
	public void scoreGame();
}
