package client;

import interfaces.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * I found this online and have adapted it for our interface
 * http://subversion.assembla.com/svn/rack-o/
 * I think it is optimizing for a flat, maximal spread distribution
 * of cards in the rack
 * @author Kyle Thompson
 */
public class PlayerKyleHard extends Player{	
	private final ArrayList<Integer> anchorPoints = new ArrayList();
	private final ArrayList<Range> myRanges = new ArrayList();
	private int[] oldRack;
	private final int rack_size;
	
	public PlayerKyleHard(int rack_size){
		this.rack_size = rack_size;
		oldRack = new int[rack_size];
		Arrays.fill(oldRack,0);
	}

	@Override
	public int play() {
		//get the rack
		int[] tmpRack = rack.getCards();
		
		//initialize the anchor points and ranges if not done already
		if (!Arrays.equals(oldRack,tmpRack)){
			anchorPoints.clear();
			myRanges.clear();
			initialize();
			calculateRanges();
		}
		
		//look at the top of the discard pile and decide which range it will fit in
		int topDiscard = game.deck.peek(true);
		int targetRange = -1;
		for (int i = 0; i < myRanges.size(); i++)
		{
			Range tmpRange = (Range) myRanges.get(i);
			if (topDiscard >= tmpRange.getLowEnd() && topDiscard <= tmpRange.getHighEnd())
			{
				targetRange = i;
				break; //target found, no need to keep searching
			}
		}
		
		//decide to draw from discard or deck (targetRange == -1 -> draw pile)
		boolean fromDiscard = targetRange != -1;
		int cardDrawn = game.deck.draw(fromDiscard);
		
		//if drew from deck, see if card is useful
		if (targetRange == -1)
		{
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
		
		int slot = 0;
		if (target.getNumSlots() > 1)
		{
			int span1 = target.getHighEnd() - cardDrawn; //test card in first slot
			int span2 = (target.getHighEnd() - cardDrawn) + (cardDrawn - target.getLowEnd()); //test card in a middle
			int span3 = cardDrawn - target.getLowEnd(); //test card in last slot
			
			if (span1 >= span2 && span1 >= span3) //put in first slot
				slot = target.getStartSlot();
			else if (span2 >= span1 && span2 >= span3) //put in a middle slot
			{
				double estimate = (((double)cardDrawn)/target.getHighEnd()) * target.getNumSlots();
				int roundNum;
				if (estimate < 1)
					roundNum = 1;
				else
					roundNum = (int) Math.round(estimate);
				slot = target.getStartSlot() + roundNum - 1;	
			}
			else if (span3 >= span1 && span3 >= span2) //put in last slot
				slot = target.getStartSlot() + target.getNumSlots() - 1;
		}
		else if (target.getNumSlots() == 1)
			slot = target.getStartSlot();
		
		//replace the card in the rack
		int toDiscard = rack.swap(cardDrawn, slot-1, fromDiscard);
		
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