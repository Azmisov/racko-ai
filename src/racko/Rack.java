package racko;

import interfaces.Distribution;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controls a Racko "rack" of cards
 * If you get an assertion error, you aren't following Racko rules
 */
public class Rack {
	//Scoring constants
	public static int
		score_all = 25,			//score for having all cards in order
		score_single = 5,		//score for a single card in order
		score_bonus = 50,		//bonus score for the minimum streak
		score_bonus_fac = 2,	//bonus score multiplication factor for each additional card above minimum streak
		bonus_min = 3,			//minimum streak for bonus
		bonus_max = 6;			//maximum streak for bonus

	//If someone had a photographic memory, they could memorize where someone
	//put a -known- card in an opponenets rack; "exposed" keeps track of which
	//cards are known to other players
	private int exposed_count, maxCard;
	private final boolean[] exposed;
	private final int[] cards;
	
	/**
	 * Initializes a rack
	 * @param size the number of cards in a rack
	 */
	public Rack(int size, int max){
		exposed_count = 0;
		exposed = new boolean[size];
		cards = new int[size];
		maxCard = max;
	}
	
	/**
	 * Deals out the starting hand
	 * @param cards array of cards
	 */
	public void deal(int[] cards){
		assert(cards.length == this.cards.length);
		System.arraycopy(cards, 0, this.cards, 0, cards.length);
		//at start of game, all cards are secret
		exposed_count = 0;
		Arrays.fill(exposed, false);
	}
	/**
	 * Swaps a drawn card with one in the rack
	 * @param card a card to insert
	 * @param position where to insert the card
	 * @param fromDiscard was this card from the discard pile; if so, it will
	 * be exposed in this player's rack
	 * @return a card to discard (the old card at "position")
	 */
	public int swap(int card, int position, boolean fromDiscard){
		assert(position >= 0 && position < cards.length);
		int old = cards[position];
		cards[position] = card;
		if (exposed[position] != fromDiscard)
			exposed_count += fromDiscard ? 1 : -1;
		exposed[position] = fromDiscard;
		return old;
	}
	
	/**
	 * Checks if the rack is sorted; standard criteria for winning
	 * @return true, if the rack is sorted
	 */
	public boolean isSorted(){
		for (int i=1; i<cards.length; i++){
			if (cards[i] < cards[i-1])
				return false;
		}
		return true;
	}
	/**
	 * Gets the rack's maximum streak (num of consecutive cards); you can
	 * require streaks of certain length to win, to make the game more interesting
	 * @return the maximum streak (will always be at least one)
	 */
	public int maxStreak(){
		int max_streak = 1, cur_streak = 1;
		for (int i=1; i<cards.length; i++){
			if (cards[i] == cards[i-1]+1)
				cur_streak++;
			else{
				if (cur_streak > max_streak)
					max_streak = cur_streak;
				cur_streak = 1;
			}
		}
		return max_streak;
	}
	
	/**
	 * Returns the numbers of the cards that have been picked up from the discard pile.
	 * @param splitCard only consider cards higher/lower than this card
	 * @param splitHigher if true, considers only higher cards; false, only lower cards
	 * @return number of cards seen by all players.
	 */
	public int getVisibleCards(int splitCard, boolean splitHigher){
		int count = 0;
		for (int i=0; i < cards.length; i++){
			if (exposed[i] && (splitHigher ? cards[i] > splitCard : cards[i] < splitCard))
				count++;
		}
		return count;
	}
	/**
	 * Returns the number of cards that are visible
	 * @return exposed card count
	 */
	public int getVisibleCardCount(){
		return exposed_count;
	}
	
	/**
	 * Gets the cards in the rack.
	 * @return The cards in the rack.
	 */
	public int[] getCards(){
		return cards;
	}
	/**
	 * Gets the card at the specified index.
	 * @param index The index corresponding to the slot in the rack whose card is returned
	 * @return The card number at the index
	 */
	public int getCardAt(int index){
		assert(index >= 0 && index < cards.length);
		return cards[index];
	}
	
	//SCORING METRICS
	/**
	 * Scores the rack for points; this is the metric used for winning a game/round
	 * @param bonusMode allows bonus points for consecutive cards, provided all
	 * the cards are in order; each consecutive card beyond "bonus_min", up to
	 * "bonus_max" will multiply "score_bonus" by "score_bonus_fac"
	 * (see static Rack variables)
	 * @return the score
	 */
	public int scorePoints(boolean bonusMode){
		int score = score_single,
			bonus = 0, cur_streak = 1;
		for (int i=1; i<cards.length; i++){
			//Not all are sorted
			if (cards[i] < cards[i-1])
				return score;
			else{
				score += score_single;
				//Calculate streaks, for bonus mode
				if (bonusMode){
					if (cards[i] == cards[i-1]+1)
						cur_streak++;
					else{
						if (cur_streak >= bonus_min){
							if (cur_streak > bonus_max)
								cur_streak = bonus_max;
							cur_streak -= bonus_min;
							bonus += Math.pow(score_bonus_fac, cur_streak)*score_bonus;
						}
						cur_streak = 1;
					}
				}
			}
		}
		//This person is a winner! (bonus is 0, if bonusMode is false)
		return score + score_all + bonus;
	}
	/**
	 * Returns the largest sum of all usable sequences
	 *	"usable" sequences are ones that could be used for a winning rack:
	 *		{4 1 3 6} gives a score of 2, since the 1 in the {1 3 6} sequence is
	 *		unusable for a winning rack (e.g. there is no card less than 1 to fill the four's spot)
	 *  if two usable sequences cannot be used together, the largest one is returned
	 *  otherwise, if they are both usable, their lengths will be summed
	 * @return largest usable sequence score
	 */
	public int scoreSequence(){
		int s = cards.length;
		//Hold cards in each sequence we're considering
		int[][] seq_cards = new int[s][s];
		//For each sequence, where each card lies in the rack (necessary to filter for "usable" sequences)
		int[][] seq_idx = new int[s][s];
		//Length of each sequence
		int[] seq_len = new int[s];
		//How many sequences are we considering and the max sequence length
		int seq_count = 0, max_len = 0;
		
		//Check each next card to see if it can be added to a sequence
		//If it cannot, create a new sequence (we only care about the "maximum" subsequences, so
		//there will only be one branch per added card; e.g. [5,6,7] and [1,2,7] are treated the same
		//since they both end in 7 and have length of three; we branch on the one with largest length)
		for (int i=0; i<s; i++){
			int card = cards[i];
			//Make sure there is enough "usable" space above this 
			if (maxCard-card < s-i-1)
				continue;
			//If we can't find any sequences that can prepend this card, we create a new branch
			//(hence the initial new_len/seq vars)
			int new_len = 0, new_seq = 0;
			for (int j=0; j<seq_count; j++){
				//Find the longest subset of the sequence that can be used
				//with this card; since the sequence is sorted, this is O(n) worst case
				//We only care about sequences greater than what we've seen already, hence the "k>new_len"
				for (int k = seq_len[j]; k>new_len; k--){
					int comp = seq_cards[j][k-1];
					//When we encounter a zero, this part of the sequence is the same as one we saw
					//earlier; we don't copy the earlier sequence to this one, because it would be redundant
					if (comp == 0) break;
					//This card is in ascending order in position "k"
					//Also make sure there is enough "usable" space in between the two cards
					if (comp < card && card-comp >= i-seq_idx[j][k-1]){
						new_seq = j;
						new_len = k;
						break;
					}
				}
			}
			//If this is the first item in the sequence, check for "usable" space below this
			if (card-1 < i)
				continue;
			//Create a new sequence, if we couldn't add it to the end of one
			if (seq_len[new_seq] > new_len)
				new_seq = seq_count++;
			//Add the card to the appropriate sequence
			seq_cards[new_seq][new_len] = card;
			seq_idx[new_seq][new_len] = i;
			if (++new_len > max_len)
				max_len = new_len;
			seq_len[new_seq] = new_len;
		}
		
		//Return the maximum usable sequence size
		return max_len;
	}
	/**
	 * Gives the sum squared error of the rack's distribution
	 * @return 
	 */
	public double scoreSSE(Distribution d){
		//TODO
		return 0;
	}
	/**
	 * Gives the sum squared error of the rack's distribution,
	 * without penalizing for usable sequences
	 * @return 
	 */
	public double scoreAdjustedSSE(Distribution d){
		//TODO
		return 0;
	}
	
	public String toString(){
		return Arrays.toString(cards);
	}
}
