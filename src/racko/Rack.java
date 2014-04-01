package racko;

import interfaces.Distribution;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Controls a Racko "rack" of cards
 * If you get an assertion error, you aren't following Racko rules
 */
public class Rack {
	//If unbiased, it will save all long usable sequences in the lus_cache
	// (as opposed to saving just the "longest" ones)
	public static boolean SCORE_UNBIASED = false;
	
	//The game we're associated with
	private final Game game;
	//If someone had a photographic memory, they could memorize where someone
	//put a -known- card in an opponenets rack; "exposed" keeps track of which
	//cards are known to other players
	private int exposed_count;
	private final boolean[] exposed;
	private final int[] cards;
	//Longest usable sequence cache
	private UsableMetric lus_metric = null;
	private final ArrayList<LUS> lus_cache;
	private int lus_max_length;
	//Probability cache [rack_size][2], [0] = Above, [1] = Below
	private final double[][] prob_cache;
	private final boolean[] prob_cache_dirty;
	private boolean prob_cache_actual;
	private int prob_cache_memlimit, prob_cache_turn;
	
	/**
	 * Initializes a rack
	 * @param size the number of cards in a rack
	 */
	public Rack(int size, Game g){
		game = g;
		exposed_count = 0;
		exposed = new boolean[size];
		cards = new int[size];
		lus_cache = new ArrayList();
		prob_cache = new double[size][2];
		prob_cache_dirty = new boolean[size];
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
		
		//Dirty caches
		lus_cache.clear();
		Arrays.fill(prob_cache_dirty, true);
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
		
		//Dirty caches
		lus_cache.clear();
		prob_cache_dirty[position] = true;
		return old;
	}
	/**
	 * Swaps a drawn card with one in the rack (ignoring "visibility")
	 * @param card a card to insert
	 * @param position where to insert the card
	 * @return a card to discard (the old card at "position")
	 */
	public int swap(int card, int position){
		assert(position >= 0 && position < cards.length);
		return swap(card, position, exposed[position]);
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
	/**
	 * Does the rack contain this card?
	 * @param card the card to check for
	 * @return whether the rack contains this card
	 */
	public boolean contains(int card){
		for (int i=0; i<cards.length; i++){
			if (cards[i] == card)
				return true;
		}
		return false;
	}
	
	//SCORING METRICS
	/**
	 * Scores the rack for points; this is the metric used for winning a game/round
	 * @param bonusMode allows bonus points for consecutive cards, provided all
	 * the cards are in order; each consecutive card beyond "bonus_min", up to
	 * "bonus_max" will multiply "score_bonus" by "score_bonus_fac"
	 * (see static Game variables)
	 * @return the score
	 */
	public int scorePoints(boolean bonusMode){
		int score = Game.score_single,
			bonus = 0, cur_streak = 1, max_streak = 1;
		for (int i=1; i<cards.length; i++){
			//Not all are sorted
			if (cards[i] < cards[i-1])
				return score;
			else{
				score += Game.score_single;
				//Calculate streaks, for bonus mode
				if (bonusMode){
					boolean is_streak = cards[i] == cards[i-1]+1;
					if (is_streak){
						cur_streak++;
						if (cur_streak > max_streak)
							max_streak = cur_streak;
					}
					//Calculate the actual bonus (when streak ends or end of rack)
					if (!is_streak || i+1 == cards.length){
						if (cur_streak >= Game.bonus_min){
							if (cur_streak > Game.bonus_max)
								cur_streak = Game.bonus_max;
							cur_streak -= Game.bonus_min;
							bonus += Math.pow(Game.score_bonus_fac, cur_streak)*Game.score_bonus;
						}
						cur_streak = 1;
					}
				}
			}
		}
		//This person is a winner! (bonus is 0, if bonusMode is false)
		boolean winner = max_streak >= game.min_streak;
		return score + (winner ? Game.score_all : 0) + bonus;
	}
	/**
	 * Gives distribution error of the rack
	 * @param target target distribution
	 * @param err_weight distribution to weight errors by (optional)
	 * @return weighted average of absolute errors, normalized between 0-1
	 *  (provided distributions are within the correct ranges)
	 */
	public double scoreRackDE(Distribution target, Distribution err_weight){
		double sum = 0;
		for (int i=0; i<cards.length; i++){
			double err = Math.abs(target.eval(i) - cards[i]);
			if (err_weight != null)
				err *= err_weight.eval(i);
			sum += err;
		}
		//Min err = 0, Max err = rack_size(max_card-rack_size)
		sum /= (double) (cards.length*(game.deck.cards-cards.length));
		assert(sum >= 0 && sum <= 1);
		return sum;
	}
	/**
	 * Gives distribution error for clumps in a long usable sequences
	 * @param seq a long usable sequence to score clumps
	 * @param target target distribution
	 * @param err_weight distribution to weight errors by
	 * @return weighted average of absolute errors, normalized between 0-1
	 *  (provided distributions are within the correct ranges)
	 */
	public double scoreClumpDE(LUS seq, Distribution target, Distribution err_weight){
		//TODO
		//maybe combine this with scoreAdjustedDE and just take null for seq?
		System.out.println("TODO implement Adjusted DE");
		return 0;
	}
	/**
	 * Gives a score for the probability of drawing cards that
	 * fill in the missing slots of a sequence
	 * @param seq a long usable sequence to use for computations
	 * @param err_weight distribution to weight probabilities (optional)
	 * @param use_average averages individual probabilities for each missing slot,
	 * rather than calculating a true probability; use this if you don't want a
	 * zero probability in one slot to cancel everything out
	 * @param prob_actual see Rack.getProbabilities
	 * @param prob_memory see Rack.getProbabilities
	 * @return probability between 0-1
	 */
	public double scoreProbability(LUS seq, Distribution err_weight, boolean use_average, boolean prob_actual, int prob_memory){
		double[][] prob = getProbabilities(prob_actual, prob_memory);
		double score = use_average ? 0 : 1;
		
		//There will always be at least one clump in a sequence
		int cur_clump = seq.indexes[0];
		double cur_prob = prob[cur_clump][1];
		for (int i=0; i<cards.length; i++){
			//Don't count cards in the sequence
			if (cur_clump == i){
				//Last card in sequence, get prob higher
				if (seq.cards.length == i+1)
					cur_prob = prob[cur_clump][0];
				//Probability in between this card and the previous
				else{
					cur_clump = seq.indexes[i+1];
					cur_prob = prob[cur_clump][1] - cur_prob;
				}
				continue;
			}
			//Weight the probability
			double weight = err_weight == null ? 1 : err_weight.eval(i);
			//Add probability to score
			if (use_average)
				score += cur_prob*weight;
			else score *= Math.pow(cur_prob, weight);
		}
		
		return use_average ? score / (double) (cards.length - seq.cards.length) : score;
	}
	/**
	 * Gives a score for the density of clumps in a sequence
	 * @param seq a long usable sequence to score clumps
	 * @param err_weight distribution to weight densities (optional)
	 * @return 
	 */
	public double scoreDensity(LUS seq, Distribution err_weight){
		//TODO
		return 0;
	}

	//LONGEST USABLE SEQUENCES
	/**
	 * Holds cached longest-usable-sequence results
	 * cards = the card numbers in the sequence
	 * indexes = the rack positions of each of the cards
	 * length = the length of the sequence (length may not equal cards.length)
	 */
	public class LUS{
		public int[] cards, indexes;
		public LUS(int[] cards, int[] indexes){
			this.cards = cards;
			this.indexes = indexes;
		}
	}
	/**
	 * Returns the largest ascending sequence that is usable
	 *	"usable" sequences are ones that could be used for a winning rack:
	 *		{4 1 3 6} gives a score of 2, since the 1 in the {1 3 6} sequence is
	 *		unusable for a winning rack (e.g. there is no card less than 1 to fill the four's spot)
	 *  if two usable sequences cannot be used together, the largest one is returned
	 *  otherwise, if they are both usable, their lengths will be summed
	 * 
	 * @return the longest usable sequence; if SCORE_UNBIASED is enabled,
	 * it will return all "long" usable sequences as well
	 */
	public ArrayList<LUS> getLUS(){
		//Since this is an n^2 algorithm, we cache the results
		if (lus_cache.size() > 0)
			return lus_cache;
		
		int s = cards.length;
		//Hold cards in each sequence we're considering
		int[][] seq_cards = new int[s][s];
		//For each sequence, where each card lies in the rack (necessary to filter for "usable" sequences)
		int[][] seq_idx = new int[s][s];
		//Length of each sequence
		int[] seq_len = new int[s];
		//Sequence prefixes, if they have branched from another sequence
		//[0] = prefix_sequence [1] = prefix_length
		int[] seq_prefix_len = new int[s];
		ArrayList<Integer>[] seq_prefix_seq = new ArrayList[s];
		for (int i=0; i<s; i++)
			seq_prefix_seq[i] = new ArrayList();
		ArrayList<Integer> prefixes = new ArrayList();
		//How many sequences are we considering
		int seq_count = 0;
		//Maximum sequence length we've seen thus far
		lus_max_length = 0;
		
		//Check each next card to see if it can be added to a sequence
		//If it cannot, create a new sequence (we only care about the "maximum" subsequences, so
		//there will only be one branch per added card; e.g. [5,6,7] and [1,2,7] are treated the same
		//since they both end in 7 and have length of three; we branch on the one with largest length;
		//however, if SCORE_UNBIASED is enabled, it will keep both)
		for (int i=0; i<s; i++){
			int card = cards[i];
			//Make sure there is enough "usable" space above this 
			if (game.deck.cards-card < s-i-1 && (lus_metric == null || lus_metric.above(card, i)))
				continue;
			//If we can't find any sequences that can prepend this card, we create a new branch
			//(hence the initial new_len/seq vars)
			int new_len = 0, new_seq = 0;
			prefixes.clear();
			prefixes.add(0);
			for (int j=0; j<seq_count; j++){
				//Find the longest subset of the sequence that can be used
				//with this card; since the sequence is sorted, this is O(n) worst case
				//We only care about sequences greater than what we've seen already, hence the "k>new_len"
				for (int k = seq_len[j]; k != 0 && k>=new_len; k--){
					int comp = seq_cards[j][k-1],
						comp_idx = seq_idx[j][k-1];
					//When we encounter a zero, this part of the sequence is the same as one we saw
					//earlier; we don't copy the earlier sequence to this one, because it would be redundant
					if (comp == 0) break;
					//This card is in ascending order in position "k"
					//Also make sure there is enough "usable" space in between the two cards
					if (comp < card && card-comp >= i-comp_idx &&
						(lus_metric == null || lus_metric.between(card, i, comp, comp_idx))
					){
						//If longer than previously seen, clear previous stored vars
						if (k != new_len){
							prefixes.clear();
							new_seq = j;
							new_len = k;
						}
						prefixes.add(j);
						break;
					}
				}
			}
			//If this is the first item in the sequence, check for "usable" space below this
			if (new_len == 0 && card-1 < i && (lus_metric == null || lus_metric.below(card, i)))
				continue;
			//Create a new sequence, if we couldn't add it to the end of one
			//If there is more than one prefix, we also branch, to avoid trickiness
			if (new_len == 0 || prefixes.size() > 1 || seq_len[new_seq] > new_len){
				seq_count++;
				//Store prefix location, for deferred mem copying
				seq_prefix_len[seq_count] = new_len;
				seq_prefix_seq[seq_count].addAll(prefixes);
				new_seq = seq_count;
			}
			//Add the card to the appropriate sequence
			seq_cards[new_seq][new_len] = card;
			seq_idx[new_seq][new_len] = i;
			if (++new_len > lus_max_length)
				lus_max_length = new_len;
			seq_len[new_seq] = new_len;
		}
		
		//Cache the results
		for (int i=0; i<seq_count; i++){
			if (SCORE_UNBIASED || seq_len[i] == lus_max_length){
				//Copy missing prefix first
				int prefix_len = seq_prefix_len[i];
				for (Integer prefix: seq_prefix_seq[i]){
					System.arraycopy(seq_cards[prefix], 0, seq_cards[i], 0, prefix_len);
					//Clone data for later use
					int[] cards_clone = Arrays.copyOf(seq_cards[i], seq_len[i]),
						  idxs_clone = Arrays.copyOf(seq_idx[i], seq_len[i]);
					lus_cache.add(new LUS(cards_clone, idxs_clone));
				}
			}
		}
		
		//Return cache
		return lus_cache;
	}
	/**
	 * Return longest usable sequence length
	 * @return length of longest usable sequence
	 */
	public int getLUSLength(){
		return lus_max_length;
	}
	/**
	 * Set an optional "usability metric" to prune unwanted sequences (in addition to the default)
	 * in the subsequent calls to getLUS();  For example, probability of drawing a card
	 * above/below/between can prune non-usable sequences
	 * @param um the usability metric; null to reset to default
	 */
	public void setLUSMetric(UsableMetric um){
		lus_metric = um;
	}
	/**
	 * Specify a usablility metric for finding longest-usable-sequences
	 * Class is given a card and it's index and a boolean value is returned,
	 * whether this card could be used in a winning rack in this position
	 */
	public interface UsableMetric{
		public boolean above(int card, int idx);
		public boolean below(int card, int idx);
		//Where idx_hi is always greater than idx_lo
		public boolean between(int card_hi, int idx_hi, int card_lo, int idx_lo);
	}
	
	//PROBABILITIES
	/**
	 * Get probabilities 
	 * @param actual use estimated vs actual probabilities
	 *	set to false to simulate human play
	 * @param mem_limit if actual = false, how much memory does this person
	 *  have to remember cards (improves probability estimates)
	 *	See Deck.getProbability for details
	 * @return a list of probabilities int[rack_size][2], where
	 *  [0] = probability of drawing higher, [1] = drawing lower
	 */
	public double[][] getProbabilities(boolean actual, int mem_limit){
		//If probabilities have changed, we need to recompute all values
		int turn = game.deck.getTurns();
		if (prob_cache_turn != turn || actual != prob_cache_actual || mem_limit != prob_cache_memlimit)
			Arrays.fill(prob_cache_dirty, true);
		
		prob_cache_actual = actual;
		prob_cache_memlimit = mem_limit;
		
		for (int i=0; i<cards.length; i++){
			if (prob_cache_dirty[i]){
				prob_cache_dirty[i] = false;
				if (actual){
					prob_cache[i][0] = game.deck.getRealProbability(cards[i], true);
					prob_cache[i][1] = game.deck.getRealProbability(cards[i], false);
				}
				else{
					prob_cache[i][0] = game.deck.getProbability(cards[i], true, this, mem_limit);
					prob_cache[i][1] = game.deck.getProbability(cards[i], false, this, mem_limit);
				}
			}
		}
		
		return prob_cache;
	}
	
	@Override
	public String toString(){
		return Arrays.toString(cards);
	}
}
