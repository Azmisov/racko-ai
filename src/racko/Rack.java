package racko;

import interfaces.Distribution;
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
		//TODO Check if cards are too high to be used in a sequence
		// Not sure where to find the maximum card value in the deck.
		int bestScore = 0;
		int score = 0;
		int prev = -1;
		for (int j=0; j < cards.length; j++)
		{
			for (int i=j; i < cards.length; i++)
			{
				if (prev == -1 && cards[i] > i && cards[i] < (maxCard - (cards.length - i)))
				{
					prev = i;
					score++;
				}
				else if (cards[i] > i && cards[i] > cards[prev] && 
						cards[i] < (maxCard - (cards.length - i)))
				{
					prev = i;
					score++;
				}
			}
			if (score > bestScore)
				bestScore = score;
		}		
		return bestScore;
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
