package racko;

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
	private final boolean[] exposed;
	private final int[] cards;
	
	/**
	 * Initializes a rack
	 * @param size the number of cards in a rack
	 */
	public Rack(int size){
		exposed = new boolean[size];
		cards = new int[size];
	}
	
	/**
	 * Deals out the starting hand
	 * @param cards array of cards
	 */
	public void deal(int[] cards){
		assert(cards.length == this.cards.length);
		System.arraycopy(cards, 0, this.cards, 0, cards.length);
		//at start of game, all cards are secret
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
		assert(position > 0 && position < cards.length);
		int old = cards[position];
		cards[position] = card;
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
	 * Scores the rack for points
	 * @param bonusMode allows bonus points for consecutive cards, provided all
	 * the cards are in order; each consecutive card beyond "bonus_min", up to
	 * "bonus_max" will multiply "score_bonus" by "score_bonus_fac"
	 * (see static Rack variables)
	 * @return the score
	 */
	public int score(boolean bonusMode){
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
	 * Returns the numbers of the cards that have been picked up from the discard pile.
	 * @return The list of cards seen by all players.
	 */
	public ArrayList<Integer> getVisibleCards()
	{
		ArrayList<Integer> rval = new ArrayList<Integer>();
		
		for(int i = 0; i < cards.length; i++)
		{
			if(exposed[i] == true)
			{
				rval.add(cards[i]);
			}
		}
		
		return rval;
	}
}
