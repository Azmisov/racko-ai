package racko;

import interfaces.GUI;
import interfaces.Player;
import java.util.Arrays;
import java.util.Random;

/**
 * Controls the Racko deck; cards start with #1
 * If you get an assertion error, you aren't following Racko rules
 */
public class Deck {
	//Game constants
	protected final int cards, rack_size;	//number of cards & the rack size
	private final Player[] players;			//list of players
	private GUI gui = null;
	//Deck variables
	private int turns;
	private int draw_count, discard_count;	//cards in draw/discard pile
	private final int[] draw, discard;		//draw and discard piles
	private final boolean[] in_play;		//which cards are in play?
	private boolean
		action = false,						//false = expect draw, true = expect discard
		dealing = false,					//remove assertions if dealing cards
		has_shuffled = false,				//have we gone through all the cards in the draw pile?
		shuffling = false;					//are we currently shuffling the deck
	//Memory variables
	private final int[] memory,				//keeps track of which cards were drawn last; 0 values should be ignored
		memory_hash;						//where each card is located in the memory array; -1, if it isn't in memory
	private int memory_head,				//memory[] is a circular buffer; where does it start?
		memory_count;						//how many cards are stored in memory? (non-zero entries)
	//Random number generator:
	private static final Random rand = new Random();
	
	/**
	 * Creates a new racko deck
	 * @param rackSize how many cards per rack; must be greater than 1
	 * @param players how many players are there; must be greater than 1
	 */
	public Deck(Player[] players, int rackSize){
		//Official rules are 2-4 players and rack_size == 10
		//However, we'll relax these restrictions...
		assert(players.length > 1 && rackSize > 1);
		this.rack_size = rackSize;
		this.players = players;
		cards = rackSize*players.length + rackSize*2;
		
		//Initialize deck; counts are all zero, since we haven't called "deal()" yet
		turns = 0;
		draw_count = 0;
		discard_count = 0;
		draw = new int[cards];
		discard = new int[cards];
		in_play = new boolean[cards];
		//Initialize memory
		memory = new int[cards];
		memory_hash = new int[cards];
		memory_head = 0;
		memory_count = 0;
		has_shuffled = false;
	}
	/**
	 * Register a gui
	 * @param gui the gui to register
	 */
	protected void registerGUI(GUI gui){
		this.gui = gui;
	}
	
	/**
	 * Creates the racks for each player; this resets the deck
	 * @param racks an array of racks; must be equal to the number of players
	 * and have the correct rack size
	 */
	protected void deal(){
		//Create a new deck
		dealing = true;
		turns = 0;
		//First, reset all deck variables
		Arrays.fill(in_play, false);
		Arrays.fill(memory, 0);
		draw_count = 0;
		discard_count = cards;
		for (int i=0; i<cards; i++)
			discard[i] = i+1;
		shuffle(true);
		//Reset memory variables
		Arrays.fill(memory_hash, -1);
		memory_head = 0;
		memory_count = 0;
		has_shuffled = false;
		//Deal out the cards
		for (Player p: players){
			//Fill each rack from top to bottom
			int[] hand = new int[rack_size];
			for (int j=rack_size-1; j>=0; j--){
				//We could call the "draw()" function here, but this is more efficient
				hand[j] = draw[--draw_count];
				in_play[hand[j]-1] = true;
			}
			p.rack.deal(hand);
			p.beginRound();
		}
		action = false;
		dealing = false;
	}
	/**
	 * Shuffle the discard pile; replaces draw pile
	 * with discard pile; sets first card in draw pile
	 * as the start of the discard pile
	 */
	private void shuffle(boolean new_discard){
		//sanity check...
		assert(draw_count == 0 && discard_count != 0);
		shuffling = true;
		System.arraycopy(discard, 0, draw, 0, discard_count);
		//Simple Fisher-Yates shuffle
		for (int i=discard_count-1, idx; i>0; i--){
			idx = rand.nextInt(i + 1);
			int temp = draw[idx];
			draw[idx] = draw[i];
			draw[i] = temp;
		}
		//Put first draw card in discard pile
		draw_count = discard_count;
		discard_count = 0;
		if (new_discard)
			discard(draw(false));
		shuffling = false;
	}
	
	/**
	 * Peek at the top of a pile
	 * @param fromDiscard if true, peeks at the top of the discard pile
	 * @return the card on the top of the pile; 0, if the pile is empty
	 * (that should only happen if "discard()" has not been called yet)
	 */
	public int peek(boolean fromDiscard){
		if (fromDiscard)
			return discard_count == 0 ? 0 : discard[discard_count-1];
		return draw_count == 0 ? 0 : draw[draw_count-1];
	}
	/**
	 * Draw a card
	 * @param fromDiscard draw from the discard pile
	 */
	public int draw(boolean fromDiscard){
		//Make sure there are the correct number of cards in play
		assert(dealing || (draw_count+discard_count == rack_size*2 && !action));
		turns++;
		action = true;
		int card;
		if (fromDiscard){
			assert(discard_count != 0);
			card = discard[--discard_count];
		}
		else{
			assert(draw_count != 0);
			card = draw[--draw_count];
		}
		in_play[card-1]	= true;
		if (!shuffling && gui != null)
			gui.draw(card, fromDiscard);
		return card;
	}
	/**
	 * Discard a card that was "in play"; shuffles the deck
	 * if there are no cards in the draw pile
	 */
	protected void discard(int card){
		assert(in_play[card-1] && action);
		action = false;
		in_play[card-1] = false;
		//Reshuffle, if no cards in draw pile
		if (draw_count == 0){
			has_shuffled = true;
			shuffle(false);
		}
		//Push to stack
		discard[discard_count++] = card;
		//Remove old reference to this card in memory
		int old_loc = memory_hash[card-1];
		if (old_loc >= 0)
			memory[old_loc] = 0;
		else if (memory_count < rack_size*2)
			memory_count++;
		//Add new reference to memory
		memory_hash[card-1] = memory_head;
		memory[memory_head] = card;
		memory_head = (memory_head + 1) % cards;
		if (!shuffling && gui != null)
			gui.discard(card);
	}
	
	/**
	 * Returns the estimated probability of drawing lower/higher than the given card
	 * @param card the card
	 * @param higher if true, gets the probability of drawing higher; otherwise, probability of drawing lower
	 * @return probability of drawing a lower/higher card
	 * @param rack the current player's rack
	 * @param mem_limit limit of how many cards to remember from the discard pile
	 *	(use 0 for photographic memory; use 1 for no memory; otherwise, integer > 1 for specific memory limit)
	 */
	public double getProbability(int card, boolean higher, Rack rack, int mem_limit){
		assert(card >= 0 && rack != null);
		
		//TODO: fix the probability calculator
		//TODO: make this compute probabilities for an entire rack at once
		if (true)
			return getRealProbability(card, higher);
		
		//If we've seen every card, we know exactly what the probabilities should be
		if ((mem_limit < 1 || mem_limit >= rack_size*2) && has_shuffled)
			return getRealProbability(card, higher);
		
		/*
			Without knowing any additional info, what is the probabity of drawing a higher/lower card?
			We can represent our estimate as:
				E = (baseline - known) / (total - visible)
				baseline: number of cards lower/higher than a card in the entire deck  (where hi or lo is user specified)
				known: number of cards lower/higher we know are not in the draw pile (where hi or lo is user specified)
				total: number of cards in the deck
				visible: number of cards that we know the values of (either from people's racks, or from the discard pile)
		*/
		int baseline = higher ? cards-card : card-1,
			total = cards - rack_size;
		
		//get visible cards from current player
		int[] players_cards = rack.getCards();
		for (int i=0; i<rack_size; i++){
			if (higher ? players_cards[i] > card : players_cards[i] < card)
				baseline--;
		}
		
		//get memorized cards from discard pile
		if (mem_limit > memory_count)
			mem_limit = memory_count;
		total -= mem_limit;
		int viewed = 0, i = memory_head;
		while (true){
			if (memory[i] != 0){
				if (higher ? memory[i] > card : memory[i] < card)
					baseline--;
				if (++viewed == mem_limit)
					break;
			}
			i = i == 0 ? memory.length : i-1;
		}
		
		assert(baseline >= 0 && total >= draw_count);
		return baseline / (double) cards;
	}
	/**
	 * Gets the actual probability of drawing higher/lower than the given card
	 * @param card the card
	 * @param higher if true, gets the probability of drawing higher; otherwise, probability of drawing lower
	 * @return 
	 */
	public double getRealProbability(int card, boolean higher){
		//We haven't dealt yet, so this is just probabilities for the entire deck
		if (draw_count == 0)
			return (higher ? cards - card : card - 1) / (double) (cards-1);
		//Otherwise, get real probability
		int total = draw_count;
		for (int i=0; i<draw_count; i++){
			if (higher ? draw[i] < card : draw[i] > card)
				total--;
		}
		return total / (double) draw_count;
	}
	
	/**
	 * Gets the total number of cards in the deck, or synonymously, 
	 * gets the value of the highest numbered card in the deck
	 * @return Number of cards
	 */
	public int getMaxCard(){
		return cards;
	}
	/**
	 * How many turns have gone since the last call to deal();
	 * @return 
	 */
	public int getTurns(){
		return turns;
	}
}