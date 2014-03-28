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
	private int deadlock_count = 0, deadlock_max;
	
	@Override
	public int play() {
		//To avoid deadlock, we'll compromise and make a random move
		//This is probably a zugzwang situation, where doing so will cost us the game
		//When rack size increases, maximization tends to fail anyways
		//Hopefully it doesn't happen too often though...
		if (++deadlock_count > deadlock_max){
			STAT_badmoves++;
			deadlock_count = 0;
			deadlock_max--;
			return rack.swap(game.deck.draw(false), rand.nextInt(game.rack_size), false);
		}
		
		//Check if taking from the discard pile will improve our score
		int peek = game.deck.peek(true);
		int best_pos = maxSequence(rack, game.rack_size, peek, true);
		
		//It will improve our score, so we'll just go with the discard pile card
		boolean fromDiscard = best_pos != -1;
		int drawn = game.deck.draw(fromDiscard);
		
		//Otherwise, compute the best position for the newly drawn card
		if (!fromDiscard)
			best_pos = maxSequence(rack, game.rack_size, drawn, false);
		
		return best_pos == -1 ? drawn : rack.swap(drawn, best_pos, fromDiscard);
	}

	@Override
	public void scoreRound(boolean won, int score) {
		deadlock_count = 0;
		deadlock_max = game.rack_size+5;
	}
	
	
	/**
	 * Maximization algorithm for scoreSequence, for a given rack/game
	 * @param r the player's rack
	 * @param rack_size the size of the rack
	 * @param drawn the card that was drawn
	 * @return the position to swap with or -1, if the card should be discarded
	 */
	public static int maxSequence(Rack r, int rack_size, int drawn, boolean forceBetter){
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
		return max_score > prev_score || (!forceBetter && max_score == prev_score) ? max_pos : -1;
	}
}
