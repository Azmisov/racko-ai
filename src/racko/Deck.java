package racko;

import interfaces.Player;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Controls the Racko deck; cards start with #1
 * If you get an assertion error, you aren't following Racko rules
 */
public class Deck {
	//Game constants
	private final int cards, rack_size;
	private final Player[] players;
	//Deck variables
	private int draw_count, discard_count;	//cards in draw/discard pile
	private final int[] draw, discard;		//draw and discard piles
	private final boolean[] in_play;		//which cards are in play?
	private boolean action = false;			//false = expect draw, true = expect discard
	private final ArrayList<ActionListener> discardListeners;
	private final int max_card_number = 30;
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
		assert(players.length > 0 && rackSize > 1);
		this.rack_size = rackSize;
		this.players = players;
		cards = rackSize*players.length + rackSize*2;
		discardListeners = new ArrayList();
		
		//Initialize deck; counts are all zero, since we haven't called "deal()" yet
		draw_count = 0;
		discard_count = 0;
		draw = new int[cards];
		discard = new int[cards];
		in_play = new boolean[cards];
	}
	
	/**
	 * Creates the racks for each player; this resets the deck
	 * @param racks an array of racks; must be equal to the number of players
	 * and have the correct rack size
	 */
	protected void deal(){
		//Create a new deck
		Arrays.fill(in_play, false);
		draw_count = 0;
		discard_count = cards;
		for (int i=0; i<cards; i++)
			discard[i] = i+1;
		shuffle();
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
		}
		action = false;
	}
	/**
	 * Shuffle the discard pile; replaces draw pile
	 * with discard pile; sets first card in draw pile
	 * as the start of the discard pile
	 */
	private void shuffle(){
		//sanity check...
		assert(draw_count == 0 && discard_count != 0);
		System.arraycopy(discard, 0, draw, 0, discard_count);
		//Simple Fisher-Yates shuffle
		for (int i=discard_count-1, idx; i>0; i--){
			idx = rand.nextInt(i + 1);
			int temp = draw[idx];
			draw[idx] = draw[i];
			draw[i] = temp;
		}
		//Put first draw card in discard pile
		draw_count = discard_count-1;
		discard_count = 1;
		discard[0] = draw[draw_count];
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
		assert(draw_count+discard_count == rack_size*2 && !action);
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
		return card;
	}
	/**
	 * Discard a card that was "in play"; shuffles the deck
	 * if there are no cards in the draw pile
	 */
	public void discard(int card){
		assert(in_play[card-1] && action);
		action = false;
		in_play[card-1] = false;
		//Push to stack
		discard[discard_count++] = card;
		//Reshuffle, if no cards in draw pile
		if (draw_count == 0)
			shuffle();
		//Notify discard listeners that the turn has ended
		for (ActionListener l: discardListeners)
			l.actionPerformed(null);
	}
	
	/**
	 * Returns the probability that a card higher than the number given will be drawn.
	 * @param cardNumber The card number.
	 * @param playerRack The rack of the player that called this method.
	 * @return The probability that a card higher than the input will be drawn.
	 */
	public double getProbablityHigher(int cardNumber, Rack playerRack){
		assert(cardNumber >= 0);
		assert(playerRack != null);
		
		double rval = 0.0;
		double baseAccuracy = max_card_number-cardNumber;
		ArrayList<Integer> visibleCards = new ArrayList<Integer>();
		
		//get visible cards from current player
		for(int i = 0; i < playerRack.getSize(); i++){
			visibleCards.add(playerRack.getCardAt(i));
		}
		
		//get other players visible cards
		for(int i = 0; i < players.length; i++){
			
			//the player we're looking at is not the player that called the method
			if(players[i].rack != playerRack){
				visibleCards.addAll(players[i].getPublicCards());
			}
		}
		
		for(int i = 0; i < visibleCards.size(); i++){
			if(visibleCards.get(i) > cardNumber){
				baseAccuracy--;
			}
		}
		
		rval = (baseAccuracy/draw_count);
		
		return rval;
	}
	
	/**
	 * Returns the probability that a card lower than the number given will be drawn.
	 * @param cardNumber The card number.
	 * @param playerRack The rack of the player that called this method.
	 * @return The probability that a card lower than the input will be drawn.
	 */
	public double getProbablityLower(int cardNumber, Rack playerRack){
		assert(cardNumber >= 0);
		assert(playerRack != null);
		
		double rval = 0.0;
		double baseAccuracy = cardNumber;
		ArrayList<Integer> visibleCards = new ArrayList<Integer>();
		
		//get visible cards from current player
		for(int i = 0; i < playerRack.getSize(); i++){
			visibleCards.add(playerRack.getCardAt(i));
		}
		
		//get other players visible cards
		for(int i = 0; i < players.length; i++){
			
			//the player we're looking at is not the player that called the method
			if(players[i].rack != playerRack){
				visibleCards.addAll(players[i].getPublicCards());
			}
		}
		
		for(int i = 0; i < visibleCards.size(); i++){
			if(visibleCards.get(i) < cardNumber){
				baseAccuracy--;
			}
		}
		
		rval = (baseAccuracy/draw_count);
		
		return rval;
	}
	
	/**
	 * Listen for a discard action; this signifies the completion of a player's turn
	 * @param listener 
	 */
	protected void addDiscardListener(ActionListener listener){
		discardListeners.add(listener);
	}
}