package racko;

import interfaces.Distribution;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Controls a Racko "rack" of cards
 * If you get an assertion error, you aren't following Racko rules
 */
public class Rack {
	//If unbiased, it will save all long usable sequences in the lus_cache
	// (as opposed to saving just the "longest" ones)
	public static boolean SCORE_UNBIASED = true;
	
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
	private ArrayList<LUS> lus_cache;
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
		//Max err formula was computed from the rack [n, n-1, n-2, ... 3, 2, 1], where n is the max card in deck
		sum /= (double) (cards.length*(game.deck.cards-cards.length));
		assert(sum >= 0 && sum <= 1);
		return sum;
	}
	/**
	 * Gives distribution error for clumps in a long usable sequences
	 * Uses the error for center of each clump, weighted by how large each clump is
	 * @param seq a long usable sequence to score clumps
	 * @param target target distribution
	 * @param err_weight distribution to weight errors by
	 * @return weighted average of absolute errors, normalized between 0-1
	 *  (provided distributions are within the correct ranges)
	 */
	public double scoreClumpDE(LUS seq, Distribution target, Distribution err_weight){
		double sum = 0, interpolate = (double) cards.length - 1;
		//Current clump size
		int clump_len = 0, cur_clump = seq.indexes[0];
		//We just use the centers of the clumps, rather than every card in the rack
		for (int i=1; i<seq.cards.length; i++){
			boolean is_clump = seq.indexes[i] == cur_clump+1;
			if (is_clump) clump_len++;
			
			//End of clump, compute 
			if (!is_clump || i+1 == seq.cards.length){
				//If this clump is at the very beginning/end of the rack, we use
				//the extremes as the center points; this "stretches" out the
				//unfilled space, so to speak
				boolean is_first = cur_clump-clump_len == 0,
						is_last = cur_clump == cards.length-1;
				double center;
				if (is_first && !is_last)
					center = 0;
				else if (is_last && !is_first)
					center = cards.length-1;
				else center = cur_clump-clump_len/2.0;
				//Compute the "lower" average (since center could be +0.5)
				int lo = (int) center;
				double err_lo = Math.abs(target.eval(lo) - seq.cards[lo]);
				if (err_weight != null)
					err_lo *= err_weight.eval(lo);
				//If center is in between two cards, we'll need to average their errors
				if (lo != center){
					double err_hi = Math.abs(target.eval(lo+1) - seq.cards[lo+1]);
					if (err_weight != null)
						err_hi *= err_weight.eval(lo+1);
					//Average the two
					err_lo = (err_lo+err_hi)/2.0;
				}
				//Weight the error based on how large the clump is
				//Error for a clump of size "cards.length", will be scaled x2
				err_lo += err_lo*clump_len/interpolate;
				sum += err_lo;
				//Reset vars
				clump_len = 0;
			}
			
			cur_clump = seq.indexes[i];
		}
		
		//Normalize error
		//Maximum error is when you have all clumps of length 1, that have maximum error
		//The max clumps of length 1 is (int) (cards.length+1/2)
		//The max error of all these clumps is: sum {i=0 to max_clumps-1} of {n-1-2i}
		int max_clumps = (cards.length+1)/2;
		sum /= (double) (max_clumps*(game.deck.cards-max_clumps));
		assert(sum >= 0 && sum <= 1);
		return sum;
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
		if (use_average)
			score /= (double) (cards.length - seq.cards.length);
		assert(score >= 0 && score <= 1);
		return score;
	}
	/**
	 * Gives a score for the density of clumps in a sequence;
	 * 0: clumps of length 1
	 * 0-1: clumps that are not in a perfect streak (1,6,8)
	 * 1: clumps in perfect streak sequence (1,2,3)
	 * @param seq a long usable sequence to score clumps
	 * @param err_weight distribution to weight densities (optional)
	 * @param loner_penalty incur penalty for clumps of length 1 (0=no affect, 1=equivalent to worst density)
	 * @return density score (average of all clump scores), between 0-1
	 */
	public double scoreDensity(LUS seq, Distribution err_weight, int loner_penalty){
		double score = 0, count = 0,
			interpolate = 1 - cards.length;
		
		//first item in clump
		boolean first = true;
		int cur_clump = seq.indexes[0];
		for (int i=1; i<seq.cards.length; i++){
			//These two cards are in a clump
			//Scoring is interpolated linearly, based on distance between cards
			if (seq.indexes[i] == cur_clump+1){
				first = false;
				double density = (seq.cards[i] - seq.cards[cur_clump] - cards.length) / interpolate;
				//Since this is really the density "in between cards", we subtract .5
				if (err_weight != null)
					density *= err_weight.eval(i-.5);
				score += density;
				count++;
			}
			//Penalize loner clumps (only one card in the clump)
			else if (first){
				double penalty = loner_penalty;
				if (err_weight != null)
					penalty *= err_weight.eval(i);
				count += penalty;
			}
			cur_clump = seq.indexes[i];
		}
		if (count > 0)
			score /= count;
		assert(score >= 0 && score <= 1);
		return score;
	}

	//LONGEST USABLE SEQUENCES
	/**
	 * Returns a list of ascending sequences that are usable
	 *	"usable" sequences are ones that could be used for a winning rack:
	 *		{4 1 3 6} gives a score of 2, since the 1 in the {1 3 6} sequence is
	 *		unusable for a winning rack (e.g. there is no card less than 1 to fill the four's spot)
	 *  if two usable sequences cannot be used together, the largest one is returned
	 *  otherwise, if they are both usable, their lengths will be summed
	 * 
	 * @return a list of usable sequences
	 */
	public ArrayList<LUS> getLUS(){
		//Since this is like an n^2 algorithm, we cache the results
		if (!lus_cache.isEmpty())
			return lus_cache;			
		
		//Get a tree of all possible long, usable, ascending sequences
		LUSTree root = new LUSTree(0, 0);
		for (int i=0; i<cards.length; i++)
			root.insert(new LUSTree(cards[i], i));
		
		//Convert the tree to a set of sequence arrays
		lus_cache = root.linearize();
		lus_max_length = 0;
		for (LUS seq: lus_cache){
			if (seq.cards.length > lus_max_length)
				lus_max_length = seq.cards.length;
		}
		return lus_cache;
	}
	/**
	 * Return longest usable sequence length
	 * @return length of longest usable sequence
	 */
	public int getLUSLength(){
		//Run the LUS computations, if they haven't been done yet
		if (lus_cache.isEmpty())
			getLUS();
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
	
	/**
	 * Holds cached longest-usable-sequence results
	 * cards = the card numbers in the sequence
	 * indexes = the rack positions of each of the cards
	 * length = the length of the sequence (length may not equal cards.length)
	 */
	public static class LUS{
		public int[] cards, indexes;
		public LUS(int[] cards, int[] indexes){
			this.cards = cards;
			this.indexes = indexes;
		}
	}
	/**
	 * Holds a tree that can be used to construct every
	 * usable sequence, through a depth first search
	 */
	private static class LUSTree{
		private static final ArrayList<ArrayList<LUSTree>> seqs = new ArrayList();
		public ArrayList<LUSTree> branches;
		public int card, index;
		//Keep track of insertion results, so we don't go back to the same node twice
		private int build_id;
		private boolean build_result;
		
		public LUSTree(int card, int index){
			this.card = card;
			this.index = index;
			branches = new ArrayList();
			build_id = 0;
		}
		
		/**
		 * Add a card to the tree
		 * @param node a leaf node (a card)
		 */
		public void insert(LUSTree node){
			build_id = node.index+1;
			//The new card cannot be inserted here (wouldn't be sorted)
			if (card != 0 && node.card < card)
				build_result = false;
			else{
				build_result = false;
				//Check to see if it can be inserted somewhere further
				for (LUSTree n: branches){
					//This node has already been visited
					if (n.build_id == build_id){
						if (n.build_result)
							build_result = true;
					}
					else{
						n.insert(node);
						if (n.build_result)
							build_result = true;
					}
				}
				//If not, we'll insert it here
				if (!build_result){
					branches.add(node);
					build_result = true;
				}
			}
		}
		/**
		 * Convert the tree into linearized sequences
		 *  (topological sorts)
		 * @return a list of sequences
		 */
		public ArrayList<LUS> linearize(){
			//Linearize the tree
			seqs.clear();
			linearize_recursive(new ArrayList());
			
			//Create LUS objects from each linearization
			ArrayList<LUS> lus = new ArrayList(seqs.size());
			for (ArrayList<LUSTree> seq: seqs){
				int size = seq.size(), i = 0;
				int[] cards = new int[size], indexes = new int[size];
				for (LUSTree n: seq){
					cards[i] = n.card;
					indexes[i++] = n.index;
				}
				lus.add(new LUS(cards, indexes));
			}
			return lus;
		}
		private void linearize_recursive(ArrayList<LUSTree> seq){
			if (card != 0)
				seq.add(this);
			//This is the end of a sequence
			if (branches.isEmpty()){
				seqs.add(seq);
				return;
			}
			//Otherwise, branch
			int len = branches.size(), i = 0;
			for (LUSTree node: branches){
				//We need to copy the sequence, so it isn't overriden
				if (len != ++i)
					node.linearize_recursive(new ArrayList(seq));
				//We can pass along the original list to one child
				else node.linearize_recursive(seq);
			}
		}
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
