package client;

import interfaces.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import racko.DataInstance;
import racko.Game;
import racko.Rack;

/**
 * I found this online and have adapted it for our interface
 * http://subversion.assembla.com/svn/rack-o/
 * I think it is optimizing for a flat, maximal spread distribution
 * of cards in the rack; it has an optional reinforcement learning mode
 * @author Kyle Thompson
 */
public class PlayerKyle extends Player{
	private final ArrayList<Integer> anchorPoints = new ArrayList();
	private final ArrayList<Range> myRanges = new ArrayList();
	private static int rack_size;
	private int[] oldRack;
	//Reinforcement learning
	private static final ArrayList<Decision> myDecisions = new ArrayList();
	private static int[][] weights;
	private final boolean use_reinforcement;
	//Stopping criteria for reinforcement learning
	private static final StoppingCriteria RI_stop = new StoppingCriteria();
	private static boolean done_learning;
	public static boolean save_moves = false;
	
	public static ArrayList<DataInstance> play_history = new ArrayList<DataInstance>();
	private ArrayList<DataInstance> game_play_history = new ArrayList<DataInstance>();
	public static ArrayList<DataInstance> draw_history = new ArrayList<DataInstance>();
	private ArrayList<DataInstance> game_draw_history = new ArrayList<DataInstance>();
	
	
	/**
	 * Create Player Kyle
	 * @param reinforce use reinforcement learning?
	 */
	public PlayerKyle(boolean reinforce){
		use_reinforcement = reinforce;
	}

	@Override
	public void register(Game g, Rack r) {
		//Default registration
		super.register(g, r);
		
		//If game configuration has changed, we need to reset our learned reinforcement weights
		rack_size = g.rack_size;
		if (weights == null || weights.length != rack_size+1 || weights[0].length != g.card_count+1){
			oldRack = new int[rack_size];
			weights = new int[rack_size+1][g.card_count+1];
			//reset stopping criteria
			done_learning = false;
			RI_stop.reset();
		}
	}
	@Override
	public void beginRound(){
		anchorPoints.clear();
		myRanges.clear();
		myDecisions.clear();
		initialize();
		calculateRanges();
	}
	@Override
	public void scoreRound(boolean won, int score) {
		//If we won the round, update the weights for reinforcement learning
		if (won) {
			updateWeights();
			
			if(save_moves == true){
				play_history.addAll(game_play_history);
				draw_history.addAll(game_draw_history);
			}
			
		}
		
		
		if(save_moves == true){
			game_play_history.clear();
			game_draw_history.clear();
		}
		
	}
	@Override
	public void epoch(){
		super.epoch();
		//Reinforcement learning stopping criteria
		if (!done_learning){
			done_learning = RI_stop.epoch(this);
			if (done_learning)
				System.out.println("PlayerKyle: Done learning!");
		}
	}
	@Override
	public int play() {
		//get the rack
		int[] tmpRack = rack.getCards();
		
		//look at the top of the discard pile and decide which range it will fit in
		int topDiscard = game.deck.peek(true);
		int targetRange = -1;
		for (int i = 0; i < myRanges.size(); i++){
			Range tmpRange = (Range) myRanges.get(i);
			if (topDiscard >= tmpRange.getLowEnd() && topDiscard <= tmpRange.getHighEnd()){
				targetRange = i;
				break; //target found, no need to keep searching
			}
		}
		
		//decide to draw from discard or deck (targetRange == -1 -> draw pile)
		boolean fromDiscard = targetRange != -1;
		int cardDrawn = game.deck.draw(fromDiscard);
		
		//if drew from deck, see if card is useful
		if (targetRange == -1){
			for (int i = 0; i < myRanges.size(); i++)
			{
				Range tmpRange = (Range) myRanges.get(i);
				if (cardDrawn > tmpRange.getLowEnd() && cardDrawn < tmpRange.getHighEnd())
				{
					targetRange = i;
					break; //target found, no need to keep searching
				}
			}
		}
		
		//discard and exit if the card is still useless
		if (targetRange == -1)
			return cardDrawn;
		
		//decide which slot the card goes in given the range
		Range target = (Range) myRanges.get(targetRange);
		
		//decide which slot the card goes in given the range
		int slot = use_reinforcement ? decideSlotReinf(target, cardDrawn) : decideSlotClassic(target, cardDrawn);			
		
		
		if(save_moves == true) {
			int num_features = rack_size + 1;
			DataInstance pdi = new DataInstance(num_features);
			pdi.addFeature(rack.getCards(), game.card_count);
			pdi.addFeature(cardDrawn, game.card_count);
			pdi.setOutput(slot,1);
			game_play_history.add(pdi);
			
			DataInstance ddi = new DataInstance(game.rack_size*3 + 1);
			//Rack
			int[] cur_rack = rack.getCards();
			ddi.addFeature(cur_rack, game.card_count);
			double[] pHigh = new double[game.rack_size],
					pLow = new double[game.rack_size];
			for (int i=0; i < game.rack_size; i++){
				pHigh[i] = game.deck.getProbability(cur_rack[i], true, rack, 0);
				pLow[i] = game.deck.getProbability(cur_rack[i], false, rack, 0);
			}
			ddi.addFeature(pHigh, 1);
			ddi.addFeature(pLow, 1);
			
			int discard = game.deck.peek(true);
			ddi.addFeature(discard, game.card_count);
			ddi.output = fromDiscard ? 0.0 : 1.0;
			game_draw_history.add(ddi);
		}
		
		//replace the card in the rack
		int toDiscard = rack.swap(cardDrawn, slot-1, fromDiscard);
		
		//add decision
		myDecisions.add(new Decision(slot, cardDrawn));
		
		//create new ranges
		if (!anchorPoints.contains (new Integer(slot)))
			anchorPoints.add (new Integer(slot));
		Collections.sort(anchorPoints); //sort the collection
		myRanges.clear(); //clear ranges
		calculateRanges(); //recalculate ranges
		
		//copy the current rack into the old rack for comparison later
		oldRack = Arrays.copyOf(rack.getCards(), rack_size);
		
		return toDiscard;
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
	private void initialize(){	
		//get rack and print it
		int[] tmpRack = rack.getCards();
		
		//analyze the rack to find the anchor points
		//if the card in slot "i" fits roughly the flat, maximal spread distribution, it is an anchor
		int slotLow = 1, slotHigh;
		for (int i = 1; i <= rack_size; i++){
			slotHigh = i * (game.deck.getMaxCard() / rack_size);
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
}