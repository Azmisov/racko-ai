/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import NeuralNetworks.Network;
import interfaces.Player;
import java.util.Arrays;
import java.util.Random;
import racko.DataInstance;

/**
 * Using TD-backprop (Temporal Difference) as was used for
 * TD Backgammon AI (Gerald Tesauro, 1991)
 * @author isaac
 */
public class PlayerTDAI extends Player{
	private static final Random RAND = new Random();
	//TD network
	private static final double LEARN_RATE = .15;
	private static final int[]
		net_layers = new int[]{15, 30, 1};
	private static final Network
		net = new Network(net_layers);
	//Stored score values
	private DataInstance data_prev = null, data_cur;
	private double score_prev, score_cur;

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
			net.compute(data_prev.inputs);
			net.trainBackprop(LEARN_RATE, new double[]{score_cur});
		}
		data_prev = data_cur;
		score_prev = score_cur;
		
		//Find the move that maximizes predicted score
		int max_slot = 0;
		double max_score = 0,
				probHi = game.deck.getRealProbability(drawn, true),
				probLo = game.deck.getRealProbability(drawn, false);
		double[] inputs = Arrays.copyOf(data_cur.inputs, data_cur.inputs.length);
		//Replace the drawn card whith each value in the rack
		for (int i=0; i<game.rack_size; i++){
			int i1 = game.rack_size + i,
				i2 = game.rack_size*2 + i;
			//Swap the card and replace input features
			int swapped = rack.swap(drawn, i);
			inputs[i1] = probHi;
			inputs[i2] = probLo;
			//Score of this move
			net.compute(inputs);
			double temp_score = net.getOutput(0);
			if (temp_score > max_score){
				max_score = temp_score;
				max_slot = i;
			}
			//Undo the swap
			rack.swap(swapped, i);
			inputs[i1] = data_cur.inputs[i1];
			inputs[i2] = data_cur.inputs[i2];
		}
		
		//Make the actual move
		int discard = rack.swap(drawn, max_slot, fromDiscard);
		return discard;
	}

	@Override
	public void scoreRound(boolean won, int score){
		//Train based on win/loss
		double[] output = new double[]{won ? 1 : 0};
		net.compute(data_prev.inputs);
		net.trainBackprop(LEARN_RATE, output);
		
		net.compute(data_cur.inputs);
		net.trainBackprop(LEARN_RATE, output);
	}
	
	private DataInstance getInputs(){
		DataInstance data = new DataInstance(game.rack_size*3);
		//Rack
		int[] cur_rack = rack.getCards();
		//Probabilities
		double[] pHigh = new double[game.rack_size],
				pLow = new double[game.rack_size];
		for (int i=0; i < game.rack_size; i++){
			pHigh[i] = game.deck.getProbability(cur_rack[i], true, rack, 0);
			pLow[i] = game.deck.getProbability(cur_rack[i], false, rack, 0);
		}
		//Insert into datainstance object
		data.addFeature(cur_rack, game.card_count);
		data.addFeature(pHigh, 1);
		data.addFeature(pLow, 1);
		return data;
	}
}
