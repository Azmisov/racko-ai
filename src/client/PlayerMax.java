package client;

import interfaces.Player;
import java.util.Random;
import racko.Rack;

/**
 * Maximizes some scoring function
 * @author isaac
 */
public class PlayerMax extends Player{
	private static final Random rand = new Random();
	
	@Override
	public int play() {
		boolean fromDiscard = rand.nextBoolean();
		int drawn = game.deck.draw(fromDiscard);
		int best_pos = maxSequence(rack, game.rack_size, drawn);
		return best_pos == -1 ? drawn : rack.swap(drawn, best_pos, fromDiscard);
	}
	
	/**
	 * Maximization algorithm for scoreSequence, for a given rack/game
	 * @param r the player's rack
	 * @param rack_size the size of the rack
	 * @param drawn the card that was drawn
	 * @return the position to swap with or -1, if the card should be discarded
	 */
	public static int maxSequence(Rack r, int rack_size, int drawn){
		//Replace the drawn card with each value in rack
		int prev_score = r.scoreSequence();
		int max_pos = 0, max_score = 0;
		for (int i=0; i<rack_size; i++){
			int swapped = r.swap(drawn, i);
			int temp_score = r.scoreSequence();
			if (temp_score > max_score){
				max_score = temp_score;
				max_pos = i;
			}
			r.swap(swapped, i);
		}
		return max_score >= prev_score ? max_pos : -1;
	}
}