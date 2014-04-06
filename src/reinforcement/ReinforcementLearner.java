package reinforcement;
import interfaces.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import racko.Game;
import racko.Rack;

public class ReinforcementLearner {
	private IndexingCriterion indexer;
	private State[] playStates;
	private State[] drawStates;
	private final int stoppingVisits = 1000;
	private int gamesSinceBelowMin;
	private int oldPlayState = -1;
	private int oldPlayAction = -1;
	private int oldDrawState = -1;
	private int oldDrawAction = -1;
	private int rackSize = 5;
	private String indexMode;

	public ReinforcementLearner()
	{
		indexMode = "AboveBelow";
		gamesSinceBelowMin = 0;
		indexer = createCriterion(indexMode);
		playStates = new State[indexer.maxStates()];
		drawStates = new State[indexer.maxStates()];
		for (int i=0; i < playStates.length; i++)
			playStates[i] = new State(false);
		for (int i=0; i < drawStates.length; i++)
			drawStates[i] = new State(true);
	}
	public ReinforcementLearner(BufferedReader buff)
	{
		try {
			rackSize = Integer.parseInt(buff.readLine());
			indexer = createCriterion(buff.readLine());
			playStates = new State[indexer.maxStates()];
			drawStates = new State[indexer.maxStates()];
			for (int i=0; i < playStates.length; i++)
				playStates[i] = new State(false, buff);
			for (int i=0; i < drawStates.length; i++)
				drawStates[i] = new State(true, buff);
		} catch (IOException ex) {
			Logger.getLogger(ReinforcementLearner.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	public boolean fromDiscardForReal(Game g, Rack r)
	{
		int index = indexer.index(g, r);
		int newDrawAction = drawStates[index].getBestReward();
		if (newDrawAction == 0)
			return false;
		else
			return true;
	}
	public boolean fromDiscard(Game g, Rack r)
	{
		int index = indexer.index(g, r);
		int newDrawState = index;
		int newDrawAction = drawStates[index].getLeastVisited();
		if (oldDrawState != -1)
		{
			drawStates[oldDrawState].updateReward(drawStates[newDrawState], oldDrawAction);
		}
		oldDrawState = newDrawState;
		oldDrawAction = newDrawAction;
		if (newDrawAction == 0)
			return false;
		else
			return true;
	}
	public int cardPositionForReal(Game g, Rack r, int card)
	{
		int index = indexer.index(g, r, card);
		
		return playStates[index].getBestReward();
	}
	public int cardPosition(Game g, Rack r, int card)
	{
		int index = indexer.index(g, r, card);
		
		int newPlayState = index;
		int newPlayAction = playStates[index].getLeastVisited();
		if (oldPlayState != -1)
		{
			playStates[oldPlayState].updateReward(playStates[newPlayState], oldPlayAction);
		}
		oldPlayState = newPlayState;
		oldPlayAction = newPlayAction;
		
		if (!playStates[index].allVisitedMore(stoppingVisits))
			gamesSinceBelowMin++;
		else
			gamesSinceBelowMin = 0;
		
		return playStates[index].getLeastVisited();
	}
	public int gamesSinceBelowMin()
	{
		return gamesSinceBelowMin;
	}
	public void gameEnd(boolean won)
	{
		State outcome;
		if (won)
			outcome = new WinState(true);
		else
			outcome = new LossState(true);
		if (oldPlayState != -1)
			playStates[oldPlayState].updateReward(outcome, oldPlayAction);
		if (oldDrawState != -1)
			drawStates[oldDrawState].updateReward(outcome, oldDrawAction);
		oldPlayState = -1;
		oldDrawState = -1;
		oldPlayAction = -1;
		oldDrawAction = -1;
	}
	
	public String saveString()
	{
		String toReturn = "";
		toReturn = toReturn + rackSize + "\n";
		toReturn = toReturn + indexMode + "\n";
		for (State s: playStates)
			toReturn = toReturn + s.saveString();
		for (State s: drawStates)
			toReturn = toReturn + s.saveString();
		return toReturn;
	}

	public IndexingCriterion createCriterion(String string)
	{
		if (string.equalsIgnoreCase("AboveBelow"))
			return new AboveBelowCriterion();

		return new AboveBelowCriterion();
	}


		private abstract class IndexingCriterion
		{
			public abstract int maxStates();
			public abstract int index(Game g, Rack r);
			public abstract int index(Game g, Rack r, int card);
		}
		private class AboveBelowCriterion extends IndexingCriterion
		{
			public int maxStates()
			{
				return (int)Math.pow(2,6);
			}
			public int index(Game g, Rack r)
			{
				double[][] probabilities = r.getProbabilities(false, 0);
				int index = 0;
				for (int i=0; i < probabilities.length; i++)
				{
					if (probabilities[i][0] > probabilities[i][1])
						index++;
					index *= 2;
				}
				int discard = g.deck.peek(true);
				double probHigher = g.deck.getProbability(discard, true, r, 0);
				double probLower = g.deck.getProbability(discard, false, r, 0);
				if (probHigher > probLower)
					index++;
				return index;
			}
			public int index(Game g, Rack r, int card)
			{
				double[][] probabilities = r.getProbabilities(false, 0);
				int index = 0;
				for (int i=0; i < probabilities.length; i++)
				{
					if (probabilities[i][0] > probabilities[i][1])
						index++;
					index *= 2;
				}
				double probHigher = g.deck.getProbability(card, true, r, 0);
				double probLower = g.deck.getProbability(card, false, r, 0);
				if (probHigher > probLower)
					index++;
				return index;
			}
		}
}