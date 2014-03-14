package client;

import java.util.ArrayList;
import java.util.Random;
import NeuralNetworks.Network;
import interfaces.Player;
import racko.DataInstance;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{
	private static final double LEARN_RATE = .1;
	private static final Random RAND = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	//Learning model
	private static final int RAND_LIMIT = 20;
	private static final Network
		drawNet = new Network(new int[]{16, 32, 1}),
		playNet = new Network(new int[]{6, 32, 6});
	//Statistics
	private int initialScore,
		games_played = 0,
		net_play_count;
	public int rand_count = 0;
	
	public PlayerAI(){
		super();
		drawHistory = new ArrayList();
		playHistory = new ArrayList();
	}
	
	@Override
	public int play() {
		net_play_count++;
		boolean drawFromDiscard = decideDraw();
		
		int card = game.deck.draw(drawFromDiscard);		
		int slot = decidePlay(rack, card);
		int discard = slot == -1 ? card : rack.swap(card, slot, drawFromDiscard);
		
		/*
		if (games_played == 251){
			System.out.println(playerNumber + ": drew card number: " + card);
			System.out.println(playerNumber + ": rack: " + rack.toString());
			System.out.println(playerNumber + ": discarded card number: " + discard);
		}
		*/
		
		return discard;
	}

	private boolean decideDraw(){
		boolean rval;

		DataInstance ddi = addDrawToHistory();
		if (net_play_count < RAND_LIMIT && games_played > 1000){
			drawNet.compute(ddi.inputs);
			rval = drawNet.getOutput() > .5;
		}
		else{
			//Wait until decidePlay before resetting play_count
			rval = RAND.nextBoolean();
		}
		ddi.setOutput(rval);
		
		return rval;
	}
	private int decidePlay(Rack rack, int card) {
		int rval = -1;
		
		DataInstance pdi = addPlayToHistory(card);
		if (net_play_count < RAND_LIMIT && games_played > 1000){
			playNet.compute(pdi.inputs);
			rval = playNet.getOutput()-1;
		}
		else{
			rand_count++;
			net_play_count = 0;
			rval = RAND.nextInt(game.rack_size+1) - 1;
		}
		//We use -1 to represent a discard action
		//The neural network needs a value between 0 to rack_size+1
		pdi.setOutput(rval+1, 1);
		
		return rval;
	}

	@Override
	public void beginRound(){
		net_play_count = 0;
		initialScore = rack.scoreSequence();
	}
	@Override
	public void scoreRound(boolean won, int score) {
		//System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" ROUND, score = "+score);
		games_played++;
		saveMoveHistory(won);
		
		//Reset history
		drawHistory.clear();
		playHistory.clear();
	}
	@Override
	public void scoreGame(boolean won) {
		//System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" GAME, score = "+score);
	}
	
	private DataInstance addDrawToHistory(){		
		//Calculate features
		int[] cur_rack = rack.getCards();
		double[] pHigh = new double[game.rack_size],
				pLow = new double[game.rack_size];
		for (int i=0; i < game.rack_size; i++){
			pHigh[i] = game.deck.getProbability(cur_rack[i], true, rack, 0);
			pLow[i] = game.deck.getProbability(cur_rack[i], false, rack, 0);
		}
		int discard = game.deck.peek(true);
		
		//Create the history
		DataInstance ddi = new DataInstance(game.rack_size*3 + 1);
		ddi.addFeature(cur_rack, game.card_count);
		ddi.addFeature(pHigh, 1);
		ddi.addFeature(pLow, 1);
		ddi.addFeature(discard, game.card_count);
		drawHistory.add(ddi);
		
		return ddi;
	}
	private DataInstance addPlayToHistory(int drawnCard){
		//Create the history
		DataInstance pdi = new DataInstance(game.rack_size + 1);
		pdi.addFeature(rack.getCards(), game.card_count);
		pdi.addFeature(drawnCard, game.card_count);
		playHistory.add(pdi);
		
		return pdi;
	}
	
	private void saveMoveHistory(boolean won){
		//Disregard games that didn't have any moves
		if (movesInRound == 0)
			return;
		
		//Score how well the AI did this round
		int endScore = rack.scoreSequence();
		double fac = (endScore-initialScore) / (double) game.rack_size / (movesInRound/100.0);
		double rate = LEARN_RATE * fac;		
		
		//Train for drawing
		for (DataInstance d: drawHistory){
			drawNet.compute(d.inputs);
			drawNet.trainBackprop(rate, (int) d.output);
		}
		
		//Train for playing
		for (DataInstance p: playHistory){			
			playNet.compute(p.inputs);
			playNet.trainBackprop(rate, (int) p.output);
		}
	}	
}
