package client;

import java.util.ArrayList;
import java.util.Random;
import NeuralNetworks.Network;
import interfaces.DataInstance;
import interfaces.Player;
import racko.DrawDataInstance;
import racko.PlayDataInstance;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{
	private static final Random rand = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	//Learning model
	private static final Network
		drawNet = new Network(new int[]{16, 32, 1}),
		playNet = new Network(new int[]{16, 32, 6});
	//Statistics
	private int initialScore;
	private int games_played = 0;
	
	public PlayerAI(){
		super();
		drawHistory = new ArrayList();
		playHistory = new ArrayList();
	}
	
	@Override
	public int play() {
		boolean drawFromDiscard = decideDraw();
		
		int card = game.deck.draw(drawFromDiscard);
		
		//System.out.println(playerNumber + ": drew card number: " + card);
		
		int slot = decidePlay(rack, card);
		int discard = slot == -1 ? card : rack.swap(card, slot, drawFromDiscard);
		
		//System.out.println(playerNumber + ": rack: " + rack.toString());
		//System.out.println(playerNumber + ": discarded card number: " + discard);
		
		return discard;
	}

	private boolean decideDraw(){
		boolean rval;

		DrawDataInstance ddi = addDrawToHistory();
		if (games_played > 4){
			drawNet.compute(ddi.getInputs());
			rval = drawNet.getOutput() > .5;
		}
		else{
			rval = rand.nextBoolean();
		}
		ddi.setOutput(rval);
		
		return rval;
	}
	private int decidePlay(Rack rack, int card) {
		int rval = -1;
		
		rval = rand.nextInt(game.rack_size+1) - 1;
		
		addPlayToHistory(rval);
		
		return rval;
	}

	@Override
	public void beginRound(){
		initialScore = rack.scoreSequence();
	}
	@Override
	public void scoreRound(boolean won, int score) {
		//System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" ROUND, score = "+score);
	}
	@Override
	public void scoreGame(boolean won) {
		//System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" GAME, score = "+score);
		games_played++;
		saveMoveHistory(won);
		
		//Reset history
		drawHistory.clear();
		playHistory.clear();
	}
	
	private DrawDataInstance addDrawToHistory(){
		//create a DrawDataInstance and fill it with the information
		int [] currentRack = rack.getCards();
		double [] pHigh = new double[currentRack.length];
		double [] pLow = new double[currentRack.length];
		for (int i=0; i < currentRack.length; i++)
		{
			pHigh[i] = game.deck.getProbability(currentRack[i], true, rack, 0);
			pLow[i] = game.deck.getProbability(currentRack[i], false, rack, 0);
		}
		int discard = game.deck.peek(true);
		DrawDataInstance DDI = new DrawDataInstance(currentRack, pHigh, pLow, discard);
		
		drawHistory.add(DDI);
		
		return DDI;
	}
	
	private void addPlayToHistory(int slot){
		playHistory.add(new PlayDataInstance(rack.getCards(), slot));
	}
	
	private void saveMoveHistory(boolean won){
		//Disregard games that didn't have any moves
		if (numberOfMoves == 0)
			return;
		
		//Score how well the AI did this round
		int endScore = rack.scoreSequence();
		double learningRateFactor = (endScore-initialScore) / (double) game.rack_size / (double) numberOfMoves;
		
		//Train for drawing
		for (DataInstance d: drawHistory){
			DrawDataInstance draw = (DrawDataInstance) d;

			drawNet.compute(draw.getInputs());
			drawNet.trainBackprop(
				.1*learningRateFactor,
				draw.getOutput() ? 1 : 0
			);
		}
		
		//Train for playing
		/*
		for (DataInstance p: playHistory){
			PlayDataInstance play = (PlayDataInstance) p;
			
			playNet.compute(play.getInputs());
			playNet.trainBackprop(
				.1*learningRateFactor,
				play.getOutput()
			);
		}
		*/
	}	
}
