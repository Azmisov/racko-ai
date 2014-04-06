package models;

import NeuralNetworks.Network;
import interfaces.Model;
import interfaces.Player;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import racko.DataInstance;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class ModelAI extends Model{
	private static final double LEARN_RATE = .1, EPSILON = 0.0001;
	private static final Random RAND = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	private DataInstance draw_instance, play_instance;
	//Learning model
	private final boolean USE_RAND, USE_PROB_DRAW = false, USE_PROB_PLAY = false;
	private final int RAND_LIMIT = 20, RAND_ROUNDS = 0;
	private Network drawNet = null, playNet = null;
	private final String drawNet_file, playNet_file;
	private boolean TRAIN;
	//Deep learning
	private final int DL_maxlayers = 4;
	private int DL_drawdelta, DL_playdelta, DL_layers = 0, rack_size;
	private final StoppingCriteria DL_stop = new StoppingCriteria();
	//Statistics
	private double initialScore, currentScore;
	private int games_played = 0, net_play_count, moves_in_round;
	public int rand_count = 0;
	
	/**static
	 * Load AI from stored file
	 * @param draw_file weights for draw network
	 * @param play_file weights for play network
	 * @param rack_size rack size
	 * @param train should we train the network?
	 */
	public ModelAI(String draw_file, String play_file, int rack_size, boolean train){
		USE_RAND = false;
		drawNet_file = draw_file;
		playNet_file = play_file;
		File draw_f = new File(drawNet_file),
			play_f = new File(playNet_file);
		boolean draw_loaded = false,
				play_loaded = false;
		if (draw_f.isFile()){
			try{
				drawNet = new Network(drawNet_file);
				if (drawNet.inputNodes() != calculateNodeCount(true, true, rack_size))
					throw new Exception();
				draw_loaded = true;
			} catch (Exception e){
				System.out.println("Warning!!! Could not load AI draw network weights");
			}
		}
		if (draw_f.isFile()){
			try{
				playNet = new Network(drawNet_file);
				if (playNet.inputNodes() != calculateNodeCount(true, false, rack_size))
					throw new Exception();
				play_loaded = true;
			} catch (Exception e){
				System.out.println("Warning!!! Could not load AI play network weights");
			}
		}
		if (!draw_loaded)
			newDrawNetwork();
		if (!play_loaded)
			newPlayNetwork();
		//Training
		TRAIN = train;
		if (TRAIN){
			drawHistory = new ArrayList();
			playHistory = new ArrayList();
		}
		else{
			drawHistory = null;
			playHistory = null;
		}
	}
	/**
	 * Create AI using preexisting networks
	 * @param draw_net draw network
	 * @param play_net play network
	 * @param train train the networks
	 */
	public ModelAI(Network draw_net, Network play_net, boolean train){
		USE_RAND = false;
		drawNet_file = null;
		playNet_file = null;
		TRAIN = train;
		if (draw_net == null)
			newDrawNetwork();
		else drawNet = draw_net;
		if (play_net == null)
			newPlayNetwork();
		else playNet = play_net;
		//Training
		TRAIN = train;
		if (TRAIN){
			drawHistory = new ArrayList();
			playHistory = new ArrayList();
		}
		else{
			drawHistory = null;
			playHistory = null;
		}
	}
	/**
	 * Create a new AI, with a new network
	 * @param random boolean random
	 */
	public ModelAI(boolean random){
		USE_RAND = random;
		TRAIN = !random;
		drawNet_file = null;
		playNet_file = null;
		if (random){
			drawHistory = null;
			playHistory = null;
		}
		else{
			drawHistory = new ArrayList();	
			playHistory = new ArrayList();
			newDrawNetwork();
			newPlayNetwork();
		}
	}
	private void newDrawNetwork(){
		//Draw network
		int[] layers = new int[]{
			calculateNodeCount(true, false, rack_size),
			calculateNodeCount(false, false, rack_size),
			1
		};
		drawNet = new Network(layers);
	}
	private void newPlayNetwork(){
		//Play network
		int[] layers = new int[]{
			calculateNodeCount(true, false, rack_size),
			calculateNodeCount(false, false, rack_size),
			rack_size+1
		};
		playNet = new Network(layers);
	}
	private void initDeepLearning(){
		int draw_hf = drawNet.layers.get(1).length,
			draw_hl = drawNet.layers.get(playNet.layers.size()-1).length,
			play_hf = playNet.layers.get(1).length,
			play_hl = playNet.layers.get(playNet.layers.size()-1).length;
		
		DL_layers = Math.max(drawNet.hiddenLayers(), playNet.hiddenLayers())-1;
		DL_drawdelta = (draw_hf-draw_hl)/DL_maxlayers;
		DL_playdelta = (play_hf-play_hl)/DL_maxlayers;
	}
	/**
	 * Calculate node count
	 * @param input true for input layer; false for hidden layer
	 * @param draw true for drawNet; false for playNet
	 * @param rack_size rack size
	 * @return node count for this layer
	 */
	private int calculateNodeCount(boolean input, boolean draw, int rack_size){
		if (input)
			return ((draw ? USE_PROB_DRAW : USE_PROB_PLAY) ? rack_size : rack_size*3) + 1;
		return ((draw ? USE_PROB_DRAW : USE_PROB_PLAY) ? rack_size*2 : rack_size*4);
	}
	
	@Override
	public boolean register(Game g, Rack r){
		super.register(g, r);
		return rack_size == g.rack_size;
	}
	@Override
	public boolean decideDraw(int turn){
		boolean rval;
		net_play_count++;
		moves_in_round++;

		//We only add the draw instnace if the decidePlay outcome is good
		createDrawHistory();
		if (!USE_RAND && net_play_count < RAND_LIMIT && games_played > RAND_ROUNDS){
			drawNet.compute(draw_instance.inputs);
			rval = drawNet.getOutput() > .5;
		}
		else{
			//Wait until decidePlay before resetting play_count
			rval = RAND.nextBoolean();
		}
		draw_instance.setOutput(rval);
		
		return rval;
	}	
	@Override
	public int decidePlay(int turns, int drawn, boolean fromDiscard) {
		int rval = -1;
		
		createPlayHistory(drawn);
		if (!USE_RAND && net_play_count < RAND_LIMIT && games_played > RAND_ROUNDS){
			playNet.compute(play_instance.inputs);
			rval = playNet.getOutput()-1;
		}
		else{
			rand_count++;
			net_play_count = 0;
			rval = RAND.nextInt(game.rack_size+1) - 1;
		}
		//We use -1 to represent a discard action
		//The neural network needs a value between 0 to rack_size+1
		play_instance.setOutput(rval+1, 1);
		
		//If we think this move was good, we'll keep it
		double newScore = scoreMetric();
		if (newScore-currentScore > 0){
			drawHistory.add(draw_instance);
			playHistory.add(play_instance);
		}
		currentScore = newScore;
		
		return rval;
	}
	@Override
	public void beginRound(){
		net_play_count = 0;
		moves_in_round = 0;
		rand_count = 0;
		initialScore = scoreMetric();
		currentScore = initialScore;
	}
	@Override
	public void scoreRound(boolean won, int score) {
		games_played++;
		saveMoveHistory(won);
		
		//Reset history
		drawHistory.clear();
		playHistory.clear();
	}
	
	private void createDrawHistory(){
		DataInstance ddi = new DataInstance(drawNet.inputNodes());
		//Rack
		int[] cur_rack = rack.getCards();
		ddi.addFeature(cur_rack, game.card_count);
		//Probabilities
		if (USE_PROB_DRAW){
			double[] pHigh = new double[game.rack_size],
					pLow = new double[game.rack_size];
			for (int i=0; i < game.rack_size; i++){
				pHigh[i] = game.deck.getProbability(cur_rack[i], true, rack, 0);
				pLow[i] = game.deck.getProbability(cur_rack[i], false, rack, 0);
			}
			ddi.addFeature(pHigh, 1);
			ddi.addFeature(pLow, 1);
		}
		//Top of discard
		int discard = game.deck.peek(true);
		ddi.addFeature(discard, game.card_count);
		
		draw_instance = ddi;
	}
	private void createPlayHistory(int drawnCard){
		DataInstance pdi = new DataInstance(playNet.inputNodes());
		//Rack
		int[] cur_rack = rack.getCards();
		pdi.addFeature(cur_rack, game.card_count);
		//Probabilities
		if (USE_PROB_PLAY){
			double[] pHigh = new double[game.rack_size],
					pLow = new double[game.rack_size];
			for (int i=0; i < game.rack_size; i++){
				pHigh[i] = game.deck.getProbability(cur_rack[i], true, rack, 0);
				pLow[i] = game.deck.getProbability(cur_rack[i], false, rack, 0);
			}
			pdi.addFeature(pHigh, 1);
			pdi.addFeature(pLow, 1);
		}
		
		//The card that was drawn
		pdi.addFeature(drawnCard, game.card_count);
		play_instance = pdi;
	}
	private void saveMoveHistory(boolean won){
		//Disregard games that didn't have any moves
		if (moves_in_round == 0)
			return;
		
		//Score how well the AI did this round
		double fac = scoreMetric() - initialScore;
		double rate = LEARN_RATE * fac;
		
		if (rate > .0001){
			//System.out.println("Training with "+drawHistory.size()+" instances");
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
	
	/**
	 * Scores close to 1 are really good moves; 0 is really bad moves
	 * @return the score
	 */
	private double scoreMetric(){
		return rack.getLUSLength() / (double) game.rack_size;
	}
	
	//DEEP LEARNING
	@Override
	public void epoch(Player p){
		//Deep learning stopping criteria
		//If no improvement, add another deep learning layer
		if (TRAIN && !USE_RAND && DL_layers <= DL_maxlayers && DL_stop.epoch(p)){
			DL_stop.reset();
			p.resetModel();
			deepLearn();
		}
	}
	private void deepLearn(){
		DL_layers++;
		//Add another layer
		if (DL_layers < DL_maxlayers){
			int dl = drawNet.layers.get(1).length - DL_layers*DL_drawdelta,
				pl = playNet.layers.get(1).length - DL_layers*DL_playdelta;
			//if (Game.verbose)
				System.out.println("PlayerAI: Adding DEEP LEARNING layer #"+DL_layers+" ("+dl+", "+pl+" nodes)");
			drawNet.addHiddenLayer(dl);
			playNet.addHiddenLayer(pl);
			drawNet.freeze(DL_layers);
			playNet.freeze(DL_layers);
		}
		//Unfreeze all layers (refinement stage)
		else{
			drawNet.freeze(0);
			playNet.freeze(0);
			//if (Game.verbose)
				System.out.println("PlayerAI: Beginning DEEP LEARNING refinement stage");
		}
	}
}
