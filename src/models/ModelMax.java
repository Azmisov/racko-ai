package models;

import interfaces.Model;
import racko.Rack;

/**
 * Maximizes some scoring function
 * @author isaac
 */
public class ModelMax extends Model{
	//Cached maxes
	private int cache_pos, cache_turn;
	
	public ModelMax(){}
	
	@Override
	public boolean decideDraw(int turn) {
		cache_turn = turn;
		//Check if taking from the discard pile will improve our score
		int peek = game.deck.peek(true);
		cache_pos = maxSequence(rack, game.rack_size, peek, true);
		return cache_pos != -1;
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {		
		//Compute the best position for the newly drawn card (if not cached)
		if (!fromDiscard || cache_turn != turn)
			cache_pos = maxSequence(rack, game.rack_size, drawn, false);
		return cache_pos;
	}
	
	/**
	 * Maximization algorithm for scoreSequence, for a given rack/game
	 * @param r the player's rack
	 * @param rack_size the size of the rack
	 * @param drawn the card that was drawn
	 * @param forceBetter discard, if it doesn't improve score
	 * @return the position to swap with or -1, if the card should be discarded
	 */
	public static int maxSequence(Rack r, int rack_size, int drawn, boolean forceBetter){
		//Replace the drawn card with each value in rack
		int prev_score = r.getLUSLength();
		int max_pos = 0, max_score = 0;
		for (int i=0; i<rack_size; i++){
			int swapped = r.swap(drawn, i);
			int temp_score = r.getLUSLength();
			if (temp_score > max_score){
				max_score = temp_score;
				max_pos = i;
			}
			r.swap(swapped, i);
		}
		return max_score > prev_score || (!forceBetter && max_score == prev_score) ? max_pos : -1;
	}

	@Override
	public String toString() {
		return "Max";
	}
}
