package models;

import NeuralNetworks.Network;
import interfaces.Model;
import interfaces.Player;
import java.io.File;
import java.util.ArrayList;
import racko.Game;
import racko.Rack;

/**
 * High level feature, TD score_network
 * @author isaac
 */
public class ModelDiablo extends Model {
	private static final boolean USE_COMBOS = false;
	private static final int FEATURES = 17;
	//Maximum points that can be won in a game
	private double max_points;
	//Last max-score, computed in scoreCard()
	private DataInstance old_score, last_score, base_score, discard_score;
	private int cache_pos, cache_turn = -1;
	//Neural network
	private static final double LEARN_RATE = 0.1;
	private final String score_file;
	private boolean TRAIN_score = true;
	private final ArrayList<DataInstance> train_data = new ArrayList();
	private double discard_threshold, learn_rate_decay;
	public Network score_net;
	
	/**
	 * Create new Diablo AI, loading score_network weights from file
	 * If file doesn't exist, it will create a new one
	 * @param score_file file to load score network weights
	 * @param train_score should we train the scoring network
	 */
	public ModelDiablo(String score_file, boolean train_score){
		//Try to load the network file
		this.score_file = score_file;
		TRAIN_score = train_score;
		File f = new File(score_file);
		boolean loaded = false;
		if (f.isFile()){
			try{
				score_net = new Network(score_file);
				loaded = true;
			} catch (Exception e){
				System.out.println("Warning!!! Could not load Diablo network weights");
			}
		}
		//Standard settings
		if (!loaded) newNetwork();
		//net.addHiddenLayer(30);
		//net.freeze(0);
	}
	/**
	 * Create a new Diablo AI, using a predefined score_network
	 * @param diablo the score_network to use
	 * @param train_score should we train the score network?
	 * @param train_draw should we train the draw network
	 */
	public ModelDiablo(ModelDiablo diablo, boolean train_score, boolean train_draw){
		score_file = diablo.score_file;
		score_net = diablo.score_net;
		TRAIN_score = train_score;
	}
	private void newNetwork(){
		System.out.println("Diablo: Creating a new network...");
		score_net = new Network(new int[]{FEATURES, FEATURES*2, 1});
	}

	@Override
	public boolean register(Game g, Rack r){
		super.register(g, r);
		max_points = g.maxPoints();
		discard_threshold = 1/(double) (game.rack_size*2.8169);
		learn_rate_decay = LEARN_RATE / (double) (game.rack_size*4);
		//Diablo works for any game configuration
		return true;
	}
	@Override
	public void scoreRound(boolean won, int score) {
		if (TRAIN_score && old_score != null){
			//Final target value is the final score
			old_score.output = score / max_points;
			//old_score.output = won ? 1 : 0;
			train_data.add(old_score);

			//Train the network
			//Give higher learning rate to more recent data
			double rate = LEARN_RATE - train_data.size()*learn_rate_decay;
			//double rate = .01;
			double[] target = new double[1];
			for (DataInstance d: train_data){
				//Wait until learning rate breaks above zero
				rate += learn_rate_decay;
				if (rate <= 0) continue;
				//Train this data
				target[0] = d.output;
				score_net.compute(d.inputs);
				score_net.trainBackprop(rate, target);
			}
			train_data.clear();
		}
	}
	@Override
	public void epoch(Player p) {
		if (TRAIN_score && score_file != null)
			score_net.export(score_file);
	}
	@Override
	public boolean decideDraw(int turn) {
		//Get a base score for this rack
		base_score = scoreRack(turn);
		cache_turn = turn;
		
		//See if taking from discard improves score at all
		int top_discard = game.deck.peek(true);
		cache_pos = findBestMove(turn, top_discard);		
		
		return last_score.output - discard_threshold > base_score.output;
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard){
		//If it gives us a bad score, take from draw pile
		if (!fromDiscard || cache_turn != turn){
			cache_pos = findBestMove(turn, drawn);
			//If it hurts our score, we'll discard
			if (last_score.output <= base_score.output)
				cache_pos = -1;
		}

		//Add training data
		if (TRAIN_score){
			if (old_score != null){
				old_score.output = last_score.output;
				train_data.add(old_score);
			}
			old_score = last_score;
		}
		
		return cache_pos;
	}
	
	/**
	 * Tests a card at every position in the rack and gives the best score/position
	 * @param turns turns made this round (by this player)
	 * @param card the card to insert into the rack somewhere
	 * @return position card should be placed for maximum score
	 *  actual scores are saved to "last_score" variable
	 */
	private int findBestMove(int turns, int card){
		int max_pos = 0, discard;
		last_score = null;
		//Test every possible move
		for (int i=0; i<game.rack_size; i++){
			//See if the card is usable in this position
			if (game.card_count-card < game.rack_size-i-1 || card-1 < i)
				continue;
			//Swap card with this position
			discard = rack.swap(card, i);
			//Score the rack
			DataInstance d = scoreRack(turns);
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
	 * @param turns turns made this round (by this player)
	 * @return features used, and the max score given from them
	 */
	private DataInstance scoreRack(int turns){
		DataInstance max_score = null;
		double rack_size = game.rack_size;
		
		//Loop through every long usable sequence for this rack
		double rack_de = rack.scoreRackDE(game.dist_flat, null),
				rack_de_skew = rack.scoreRackDE(game.dist_flat, game.dist_skew);
		for (Rack.LUS lus: rack.getLUS(USE_COMBOS)){
			/* Other features to consider:
				- scoreDensityAdjacent weighted by probabilities or distribution-error
				- scoreClumpDE weighted by probabilities or density
				- scoreProbability weighted by distribution-error
				- bonusMode: may optimize for denser clumps to get bonus points
				- min_streak/rack_size: optimize for dense clumps to actually win the game

				Tree traversal techniques:
				- just take output of getLSU()
				- use probabilities/density/clumpDE as a UsableMetric for getLSU()
				- use probabilties/... to generate new sequences from getLSU() output
			
				TODO:
				- use other player's visible cards in rack to see if discarding would lose the game
				- train with deep learning enabled
				- fix probability calculator
				- train with bonus_mode & min_streak features
			*/
			DataInstance d = new DataInstance(FEATURES);
			//if closer to one, the game is close to ending; may want to get more points before game ends
			d.addFeature(turns > rack_size*2 ? rack_size*2 : turns, rack_size);
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
			//minimize density error of adjacent cards of clumps in a sequence
			d.addFeature(rack.scoreDensityAdjacent(lus, null, 0), 1);
			//same as previous, except penalizes for clumps of only length 1
			d.addFeature(rack.scoreDensityAdjacent(lus, null, 1), 1);
			//minimize density error of cards to clump centroids
			d.addFeature(rack.scoreDensityCenter(lus, null), 1);

			//SKEWED FEATURES
			//All the same as before, except optimizes for a higher score if the player doesn't win
			d.addFeature(rack_de_skew, 1);
			d.addFeature(rack.scoreClumpDE(lus, game.dist_flat, game.dist_skew), 1);
			d.addFeature(rack.scoreProbability(lus, game.dist_skew, false, true, 0), 1);
			d.addFeature(rack.scoreProbability(lus, game.dist_skew, true, true, 0), 1);
			d.addFeature(rack.scoreDensityAdjacent(lus, game.dist_skew, 0), 1);
			d.addFeature(rack.scoreDensityAdjacent(lus, game.dist_skew, 1), 1);
			d.addFeature(rack.scoreDensityCenter(lus, game.dist_skew), 1);

			//Run it through the neural network
			score_net.compute(d.inputs);
			d.output = score_net.getOutput(0);
			if (max_score == null || d.output > max_score.output)
				max_score = d;
		}
		
		return max_score;
	}

	@Override
	public String toString() {
		return "Diablo";
	}
}
