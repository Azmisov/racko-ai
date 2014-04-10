package models;

import interfaces.Model;
import interfaces.Player;
import java.util.ArrayList;
import java.util.Collections;
import racko.Game;
import racko.Rack;

/**
 * I found this online and have adapted it for our interface
 * http://subversion.assembla.com/svn/rack-o/
 * I think it is optimizing for a flat, maximal spread distribution
 * of cards in the rack; it has an optional reinforcement learning mode
 * @author Kyle Thompson
 */
public class ModelKyle extends Model{
	//Rack cache
	private final ArrayList<Integer> anchorPoints = new ArrayList();
	private final ArrayList<Range> myRanges = new ArrayList();
	private int rack_size, card_count;
	private int cache_pos, cache_turn;
	//Reinforcement learning
	private final ArrayList<Decision> myDecisions = new ArrayList();
	private int[][] weights;
	private final boolean use_reinforcement;
	//Stopping criteria for reinforcement learning
	private final StoppingCriteria RI_stop = new StoppingCriteria();
	private boolean done_learning;
	
	/**
	 * Create Kyle Model
	 * @param reinforce use reinforcement learning?
	 */
	public ModelKyle(boolean reinforce){
		use_reinforcement = reinforce;
		done_learning = !reinforce;
	}
	
	@Override
	public boolean register(Game g, Rack r){
		super.register(g, r);
		rack_size = g.rack_size;
		card_count = g.card_count;
		
		//Setup reinforcement learning
		if (use_reinforcement){
			weights = new int[rack_size+1][card_count+1];
			//reset stopping criteria
			done_learning = false;
			RI_stop.reset();
		}
		return true;
	}
	@Override
	public void beginRound(){
		anchorPoints.clear();
		myDecisions.clear();
		calculateAnchors();
	}
	@Override
	public void scoreRound(boolean won, int score) {
		//If we won the round, update the weights for reinforcement learning
		if (won) updateWeights();
	}
	@Override
	public void epoch(Player p){
		//Reinforcement learning stopping criteria
		if (!done_learning){
			done_learning = RI_stop.epoch(p);
			if (done_learning)
				System.out.println("PlayerKyle: Done learning!");
		}
	}
	@Override
	public boolean decideDraw(int turn) {
		//Recualculate ranges
		myRanges.clear(); //clear ranges
		calculateRanges(); //recalculate ranges
		
		cache_turn = turn;
		//look at the top of the discard pile and decide which range it will fit in
		int topDiscard = game.deck.peek(true);
		cache_pos = findRange(true, topDiscard);
		return cache_pos != -1;
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard){		
		//see if this card is useful (or if not cached already)
		if (!fromDiscard || turn != cache_turn){
			//Recalculate ranges, if needed
			if (turn != cache_turn){
				myRanges.clear(); //clear ranges
				calculateRanges(); //recalculate ranges
			}
			cache_turn = turn;
			cache_pos = findRange(fromDiscard, drawn);
		}
		
		//discard and exit if the card is still useless
		if (cache_pos == -1)
			return -1;
		
		//decide which slot the card goes in given the range
		Range target = (Range) myRanges.get(cache_pos);
		
		//decide which slot the card goes in given the range
		int slot = use_reinforcement ? decideSlotReinf(target, drawn) : decideSlotClassic(target, drawn);
		
		//add decision
		if (use_reinforcement && !done_learning)
			myDecisions.add(new Decision(slot, drawn));
		
		//create new ranges
		if (!anchorPoints.contains (new Integer(slot)))
			anchorPoints.add (new Integer(slot));
		Collections.sort(anchorPoints); //sort the collection

		return slot-1;
	}
	
	//SLOT DECISION MAKING
	private int decideSlotReinf(Range target, int cardDrawn){
		int slot = 0;
		if (target.getHighEnd() - cardDrawn <= 5)
			slot = target.getStartSlot() + target.getNumSlots() - 1;
		else if (cardDrawn - target.getLowEnd() <= 5)
			slot = target.getStartSlot();
		else{
			int highCount = 0;
			for (int i = target.getStartSlot(); i <= target.getStartSlot() + target.getNumSlots() - 1; i++){
				if (weights[i][cardDrawn] >= highCount){
					highCount = weights[i][cardDrawn];
					slot = i;
				}
			}
		}
		return slot;
	}
	private int decideSlotClassic(Range target, int cardDrawn){
		//Original logic for the hard player
		int slot = 0;
		if (target.getNumSlots() > 1){
			int span1 = target.getHighEnd() - cardDrawn; //test card in first slot
			int span2 = (target.getHighEnd() - cardDrawn) + (cardDrawn - target.getLowEnd()); //test card in a middle
			int span3 = cardDrawn - target.getLowEnd(); //test card in last slot
			
			//put in first slot
			if (span1 >= span2 && span1 >= span3)
				slot = target.getStartSlot();
			//put in a middle slot
			else if (span2 >= span1 && span2 >= span3){
				double estimate = (((double)cardDrawn)/target.getHighEnd()) * target.getNumSlots();
				int roundNum;
				if (estimate < 1)
					roundNum = 1;
				else
					roundNum = (int) Math.round(estimate);
				slot = target.getStartSlot() + roundNum - 1;	
			}
			//put in last slot
			else if (span3 >= span1 && span3 >= span2)
				slot = target.getStartSlot() + target.getNumSlots() - 1;
		}
		else if (target.getNumSlots() == 1)
			slot = target.getStartSlot();
		return slot;
	}
	
	//REINFORCEMENT LEARNING
	private void updateWeights(){
		//update the weights matrix based on the decisions made in the round
		for (Decision d: myDecisions)
			weights[d.getSlot()][d.getCard()]++;
	}
	public class Decision{
		private int slot;
		private int card;

		public Decision(int slot, int card){
			this.slot = slot;
			this.card = card;
		}

		public int getSlot ()
		{ return slot; }

		public int getCard ()
		{ return card; }

		public void setSlot(int slot)
		{ this.slot = slot; }

		public void setCard(int card)
		{ this.card = card; }
	}
	
	//RANGE CALCULATIONS
	private void calculateAnchors(){	
		//get rack and print it
		int[] tmpRack = rack.getCards();
		
		//analyze the rack to find the anchor points
		//if the card in slot "i" fits roughly the flat, maximal spread distribution, it is an anchor
		int slotLow = 1, slotHigh;
		for (int i = 1; i <= rack_size; i++){
			slotHigh = i * (card_count / rack_size);
			if (tmpRack[i-1] >= slotLow && tmpRack[i-1] <= slotHigh)
				anchorPoints.add(new Integer(i));
			slotLow = slotHigh + 1;
		}
		
		//add index to anchor points if one of the other anchors is a consecutive of it
		/* THIS CODE DOESNT WORK FOR SOME REASON; SEE BELOW FOR REVISED VERSION
		for (int i = 0; i < anchorPoints.size(); i++)
		{
			if ((tmpRack[i+1] == tmpRack[i] + 1) && !anchorPoints.contains(new Integer(i+1)))
				anchorPoints.add(new Integer(i+1));
		}
		*/
		
		//I had to modify this section quite a bit to get it to not throw errors;
		//... not sure if this is what Kyle meant by his above comment
		for (int i=0; i<anchorPoints.size(); i++){
			int anchor = anchorPoints.get(i),
				consec_anchor = new Integer(anchor+1);
			if (anchor != rack_size && tmpRack[anchor-1] + 1 == tmpRack[anchor] && !anchorPoints.contains(consec_anchor))
				anchorPoints.add(consec_anchor);
		}
		
		Collections.sort(anchorPoints); //sort the collection if any ranges were added
	}
	private void calculateRanges(){		
		//creates ranges in which to put cards
		int[] tmpRack = rack.getCards();
		int lowEnd = 1, highEnd,
			startSlot = 1,
			numSlots = 1;
		
		if (anchorPoints.contains(new Integer(1))) //handle if slot 1 is an anchor, skip it
		{
			lowEnd = tmpRack[0] + 1;
			numSlots--;
			startSlot = 2;
		}
		
		for (int i = 2; i < rack_size; i++){
			highEnd = tmpRack[i-1] - 1; //set highEnd based on the next anchor point
			if (anchorPoints.contains(new Integer(i)))
			{
				myRanges.add(new Range(lowEnd, highEnd, startSlot, numSlots));
				startSlot = i + 1;
				lowEnd = tmpRack[i-1] + 1; //reset lowEnd
				numSlots = 0; //reset numSlots because a range has been created
			}
			else
			{
				numSlots++;
			}
		}
		
		if (anchorPoints.contains(new Integer(rack_size))) //handle if final-slot is an anchor
			highEnd = tmpRack[rack_size-1] - 1;
		else //if final-slot is not an anchor
		{
			highEnd = game.deck.getMaxCard();
			numSlots++;
		}
		myRanges.add(new Range(lowEnd, highEnd, startSlot, numSlots)); //create the final range
		
		
		//remove any ranges with zero slots in them
		ArrayList<Range> toRemove = new ArrayList();
		for (int i = 0; i < myRanges.size(); i++){
			Range tmpRange = (Range) myRanges.get(i);
			if (tmpRange.getNumSlots() == 0)
				toRemove.add(tmpRange);
		}
		myRanges.removeAll(toRemove);
	}
	private int findRange(boolean forDraw, int card){
		int targetRange = -1;
		for (int i = 0; i < myRanges.size(); i++){
			Range tmpRange = (Range) myRanges.get(i);
			int lo = tmpRange.getLowEnd(), hi = tmpRange.getHighEnd();
			if (card > lo && card < hi || (forDraw && (card == lo || card == hi))){
				targetRange = i;
				break; //target found, no need to keep searching
			}
		}
		return targetRange;
	}
	public class Range{
		private int lowEnd, highEnd, startSlot, numSlots;
		
		public Range(int lowEnd, int highEnd, int startSlot, int numSlots){
			this.lowEnd = lowEnd;
			this.highEnd = highEnd;
			this.startSlot = startSlot;
			this.numSlots = numSlots;
		}
		
		public int getLowEnd ()
		{ return lowEnd; }
		
		public int getHighEnd ()
		{ return highEnd; }
		
		public int getStartSlot ()
		{ return startSlot; }
		
		public int getNumSlots ()
		{ return numSlots; }
		
		public void setLowEnd (int newLowEnd)
		{ lowEnd = newLowEnd; }
		
		public void setHighEnd (int newHighEnd)
		{ highEnd = newHighEnd; }
		
		public void setStartSlot (int newStartSlot)
		{  newStartSlot = startSlot; }
		
		public void setNumSlots (int newNumSlots)
		{ numSlots = newNumSlots; }
	}

	@Override
	public String toString() {
		return "Kyle";
	}
}
