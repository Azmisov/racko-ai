package models;

import NeuralNetworks.Network;
import interfaces.Player;
import java.util.Arrays;
import java.util.Random;
import racko.Game;
import racko.Rack;

/**
 * Using TD-backprop (Temporal Difference) as was used for
 * TD Backgammon AI (Gerald Tesauro, 1991)
 * @author isaac
 */
public class ModelTD extends Player{
	private static final boolean USE_PROB = false;
	private static final Random RAND = new Random();
	//TD network
	private static final double LEARN_RATE = .15;
	private static int[] net_layers;
	private static Network net = null;
	private static int rack_size;
	//Deep learning
	private static final StoppingCriteria DL_stop = new StoppingCriteria();
	private static final int DL_maxlayers = 10;
	private static int DL_delta, DL_layers = 0;
	//Stored score values
	private boolean biased_play = false;
	private DataInstance data_prev = null, data_cur;
	private double score_prev, score_cur;
	private int net_play_count, games_played;

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		
		//TODO: fix this here!!!
		//Change game configuration
		if (net == null || rack_size != g.rack_size){
			rack_size = g.rack_size;
			int inputs = USE_PROB ? rack_size : rack_size*3,
				hidden = USE_PROB ? rack_size*2 : rack_size*4;
			net_layers = new int[]{inputs, hidden, 1};
			DL_delta = (net_layers[1]-net_layers[2])/DL_maxlayers;
			net = new Network(net_layers);
		}
		
		return true;
	}
	@Override
	public int play() {
		/* The temporal difference algorithm works by training a nueral network to score moves.
			We store the score before moving as "score_prev". Then, check every possible move
			(or use some branch-bound search) to find the move that maximizes the network's
			output score. Let this be "score_cur". We use this as our move for the turn.
		
			To train the network, we want "score_prev" to equal "score_cur". So we train against
			the previous input features, using "score_cur" as the target value. At the end of the
			game, we train against a value of 1 or 0, depending on a win or loss.
		
			For detailed description: http://www.cs.cornell.edu/boom/2001sp/Tsinteris/gammon.htm)
		*/
		//TODO, use TD to compute draw or discard pile
		boolean fromDiscard = RAND.nextBoolean();
		int drawn = game.deck.draw(fromDiscard);
		
		//Get current scores
		data_cur = getInputs();
		net.compute(data_cur.inputs);
		score_cur = net.getOutput(0);
				
		//Train network to predict current scores, given previous data
		if (data_prev != null){
			double output = biased_play ? rack.getLUSLength() / (double) game.rack_size : score_cur;
			net.compute(data_prev.inputs);
			net.trainBackprop(LEARN_RATE, new double[]{output});
		}
		data_prev = data_cur;
		score_prev = score_cur;
		
		//If we're stuck, pick a random move
		if (++net_play_count == 30){
			net_play_count = 0;
			STAT_badmoves++;
			int swap = RAND.nextInt(game.rack_size+1) - 1;
			return swap == -1 ? drawn : rack.swap(drawn, swap, fromDiscard);
		}
		
		//*
		//Use a predefined scoring function as a starting bias
		if (games_played < 100000 && (net_play_count == 20 || games_played % 10 == 0)){
			STAT_badmoves++;
			biased_play = true;
			int swap = ModelMax.maxSequence(rack, game.rack_size, drawn, false);
			return swap == -1 ? drawn : rack.swap(drawn, swap, fromDiscard);
		}
		//*/
		
		//Find the move that maximizes predicted score
		int max_slot = 0;
		double max_score = 0,
				probHi = game.deck.getRealProbability(drawn, true),
				probLo = game.deck.getRealProbability(drawn, false);
		double[] inputs = Arrays.copyOf(data_cur.inputs, data_cur.inputs.length);
		//Replace the drawn card with each value in the rack
		for (int i=0; i<game.rack_size; i++){
			int i1 = game.rack_size + i,
				i2 = game.rack_size*2 + i;
			//Swap the card and replace input features
			int swapped = rack.swap(drawn, i);
			if (USE_PROB){
				inputs[i1] = probHi;
				inputs[i2] = probLo;
			}
			//Score of this move
			net.compute(inputs);
			double temp_score = net.getOutput(0);
			if (temp_score > max_score){
				max_score = temp_score;
				max_slot = i;
			}
			//Undo the swap
			rack.swap(swapped, i);
			if (USE_PROB){
				inputs[i1] = data_cur.inputs[i1];
				inputs[i2] = data_cur.inputs[i2];
			}
		}
		
		//Make the actual move
		return max_score >= score_cur ? rack.swap(drawn, max_slot, fromDiscard) : drawn;
	}
	@Override
	public void beginRound() {
		net_play_count = 0;
	}
	@Override
	public void scoreRound(boolean won, int score){
		games_played++;
		
		//Train based on win/loss
		double[] output = new double[]{won ? 1 : 0};
		/*
		net.compute(data_prev.inputs);
		net.trainBackprop(LEARN_RATE, output);
		*/
		
		net.compute(data_cur.inputs);
		net.trainBackprop(LEARN_RATE, output);
	}
	
	private DataInstance getInputs(){
		DataInstance data = new DataInstance(net_layers[0]);
		//Rack
		int[] cur_rack = rack.getCards();
		data.addFeature(cur_rack, game.card_count);
		//Probabilities
		if (USE_PROB){
			double[] pHigh = new double[game.rack_size],
					pLow = new double[game.rack_size];
			for (int i=0; i < game.rack_size; i++){
				pHigh[i] = game.deck.getProbability(cur_rack[i], true, rack, 0);
				pLow[i] = game.deck.getProbability(cur_rack[i], false, rack, 0);
			}
			data.addFeature(pHigh, 1);
			data.addFeature(pLow, 1);
		}		
		return data;
	}
	
	//DEEP LEARNING
	@Override
	public void epoch(){
		super.epoch();
		
		//Deep learning stopping criteria
		//If no improvement, add another deep learning layer
		if (DL_layers <= DL_maxlayers && DL_stop.epoch(this)){
			DL_stop.reset();
			resetModel();
			deepLearn();
		}
	}
	private static void deepLearn(){
		DL_layers++;
		//Add another layer
		if (DL_layers < DL_maxlayers){
			int dl = net_layers[1] - DL_layers*DL_delta;
			//if (Game.verbose)
				System.out.println("PlayerTD: Adding DEEP LEARNING layer #"+DL_layers+" ("+dl+" nodes)");
			net.addHiddenLayer(dl);
			net.freeze(DL_layers);
		}
		//Unfreeze all layers (refinement stage)
		else{
			net.freeze(0);
			//if (Game.verbose)
				System.out.println("PlayerTD: Beginning DEEP LEARNING refinement stage");
		}
	}

	@Override
	public String toString() {
		return "TD";
	}
}
