package client;

import NeuralNetworks.Network;
import interfaces.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import racko.DataInstance;
import racko.Game;
import racko.Rack;
import racko.Rack.LUS;

/**
 * @author isaac
 */
public class PlayerDiablo extends Player{
	private static final Random RAND = new Random();
	//Last max-score, computed in scoreCard()
	private DataInstance old_score, last_score;
	//Turns made this round
	private int turns, no_progress_limit;
	//Maximum points that can be won in a game
	private double max_points;
	//Neural network
	private static final double LEARN_RATE = 0.1;
	private final ArrayList<DataInstance> train_data = new ArrayList();
	private double discard_threshold, learn_rate_decay;
	private static final Network net = new Network(new int[]{15, 30, 1});

	@Override
	public void register(Game g, Rack r) {
		super.register(g, r);
		max_points = g.maxPoints();
		discard_threshold = 1/(double) (game.rack_size*2);
		learn_rate_decay = LEARN_RATE / (double) (game.rack_size*4);
		no_progress_limit = game.rack_size*2;
	}
	@Override
	public void beginRound() {
		turns = 0;
	}
	@Override
	public void scoreRound(boolean won, int score) {
		//Final target value is the final score
		old_score.output = score / max_points;
		train_data.add(old_score);
		
		//Train the network
		//Give higher learning rate to more recent data
		double rate = LEARN_RATE - train_data.size()*learn_rate_decay;
		double[] target = new double[1];
		for (DataInstance d: train_data){
			//Wait until learning rate breaks above zero
			rate += learn_rate_decay;
			if (rate <= 0) continue;
			//Train this data
			target[0] = d.output;
			net.compute(d.inputs);
			net.trainBackprop(rate, target);
		}
		train_data.clear();
	}
	@Override
	public int play() {
		turns++;
		int drawn, pos;
		boolean fromDiscard;
		//If we haven't made progress in a while, do a random move
		if (turns % no_progress_limit == 0){
			STAT_badmoves++;
			fromDiscard = RAND.nextBoolean();
			drawn = game.deck.draw(fromDiscard);
			findBestMove(drawn);
			pos = RAND.nextInt(game.rack_size+1)-1;
		}
		//Otherwise, use regular scoring approach
		else{
			//Get a base score for this rack
			DataInstance base = scoreRack();

			//See if taking from discard improves score at all
			drawn = game.deck.peek(true);
			pos = findBestMove(drawn);
			//We use a learned weight to determine how good the draw-from-discard score has to be to accept
			//This could be much more sophisticated...
			double discard_score = last_score.output;
			fromDiscard = discard_score - discard_threshold > base.output;

			//If it gives us a bad score, take from draw pile
			if (!fromDiscard){
				drawn = game.deck.draw(false);
				pos = findBestMove(drawn);
				//If it hurts our score, we'll discard
				if (last_score.output <= base.output)
					pos = -1;
			}
			else game.deck.draw(true);
		}
		//Add training data
		if (old_score != null){
			old_score.output = last_score.output;
			train_data.add(old_score);
		}
		old_score = last_score;
		return pos == -1 ? drawn : rack.swap(drawn, pos, fromDiscard);
	}
	
	/**
	 * Tests a card at every position in the rack and gives the best score/position
	 * @param card the card to insert into the rack somewhere
	 * @return position card should be placed for maximum score
	 *  actual scores are saved to "last_score" variable
	 */
	private int findBestMove(int card){
		int max_pos = 0, discard = 0;
		last_score = null;
		//Test every possible move
		for (int i=0; i<game.rack_size; i++){
			//Swap card with this position
			discard = rack.swap(card, i);
			//Score the rack
			DataInstance d = scoreRack();
			if (last_score == null || d.output > last_score.output){
				last_score = d;
				max_pos = i;
			}
			//Undo the swap
			rack.swap(discard, i);
		}
		return max_pos;
	}
	/**
	 * Scores the rack
	 * @return features used, and the max score given from them
	 */
	private DataInstance scoreRack(){
		DataInstance max_score = null;
		double rack_size = game.rack_size;
		
		//Loop through every long usable sequence for this rack
		double rack_de = rack.scoreRackDE(game.dist_flat, null),
				rack_de_skew = rack.scoreRackDE(game.dist_flat, game.dist_skew);
		for (LUS lus: rack.getLUS()){
			/* Other features to consider:
				- scoreDensity weighted by probabilities or distribution-error
				- scoreClumpDE weighted by probabilities or density
				- scoreProbability weighted by distribution-error
				- bonusMode: may optimize for denser clumps to get bonus points
				- min_streak/rack_size: optimize for dense clumps to actually win the game

				Tree traversal techniques:
				- just take output of getLSU()
				- use probabilities/density/clumpDE as a UsableMetric for getLSU()
				- use probabilties/... to generate new sequences from getLSU() output
			*/
			DataInstance d = new DataInstance(15);
			//if closer to one, the game is close to ending; may want to get more points before game ends
			d.addFeature(turns, rack_size);
			//maximize points scored (possibly, when turn_ratio is high and sequence length is low)
			d.addFeature(rack.scorePoints(game.bonus_mode), max_points);
			//maximize usable sequence length
			d.addFeature(lus.cards.length, rack_size);
			//get a rack with flat, spread out distribution
			d.addFeature(rack_de, 1);
			//same as previous, except ignoring unusable cards
			d.addFeature(rack.scoreClumpDE(lus, game.dist_flat, null), 1);
			//maximize probability of getting a larger sequence
			d.addFeature(rack.scoreProbability(lus, null, false, true, 0), 1);
			//same as previous, except does not penalize as much for low probabilities
			d.addFeature(rack.scoreProbability(lus, null, true, true, 0), 1);
			//maximize density of clumps in a sequence
			d.addFeature(rack.scoreDensity(lus, null, 0), 1);
			//same as previous, except penalizes for clumps of only length 1
			d.addFeature(rack.scoreDensity(lus, null, 1), 1);

			//SKEWED FEATURES
			//All the same as before, except optimizes for a higher score if the player doesn't win
			d.addFeature(rack_de_skew, 1);
			d.addFeature(rack.scoreClumpDE(lus, game.dist_flat, game.dist_skew), 1);
			d.addFeature(rack.scoreProbability(lus, game.dist_skew, false, true, 0), 1);
			d.addFeature(rack.scoreProbability(lus, game.dist_skew, true, true, 0), 1);
			d.addFeature(rack.scoreDensity(lus, game.dist_skew, 0), 1);
			d.addFeature(rack.scoreDensity(lus, game.dist_skew, 1), 1);

			//Run it through the neural network
			net.compute(d.inputs);
			d.output = net.getOutput(0);
			if (max_score == null || d.output > max_score.output)
				max_score = d;
		}
		
		return max_score;
	}
}
