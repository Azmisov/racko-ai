package client;

import java.util.ArrayList;
import java.util.Random;
import NeuralNetworks.Network;
import interfaces.Player;
import racko.DataInstance;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as an artificial intelligence
 */
public class PlayerAI extends Player{
	private static final double LEARN_RATE = .1, EPSILON = 0.00001;
	private static final Random RAND = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	private DataInstance draw_instance, play_instance;
	//Learning model
	private final boolean USE_RAND, USE_PROB_DRAW = false, USE_PROB_PLAY = false;
	private static final int RAND_LIMIT = 20, RAND_ROUNDS = 0;
	private static int[] drawNet_layers, playNet_layers;
	private static Network drawNet = null, playNet = null;
	//Deep learning
	private static final int DL_maxlayers = 4;
	private static int DL_drawdelta, DL_playdelta, DL_layers = 0, rack_size;
	private static final StoppingCriteria DL_stop = new StoppingCriteria();
	//Statistics
	private double initialScore, currentScore;
	private int games_played = 0, net_play_count, moves_in_round;
	public int rand_count = 0;
	
	public PlayerAI(boolean random){
		super();
		USE_RAND = random;
		drawHistory = new ArrayList();
		playHistory = new ArrayList();
	}

	@Override
	public void register(Game g, Rack r) {
		super.register(g, r);
		
		//Change game configuration
		if (drawNet == null || rack_size != g.rack_size){
			rack_size = g.rack_size;
			//Draw network
			int inputs_d = (USE_PROB_DRAW ? rack_size : rack_size*3) + 1,
				hidden_d = USE_PROB_DRAW ? rack_size*2 : rack_size*4;
			drawNet_layers = new int[]{inputs_d, hidden_d, 1};
			DL_drawdelta = (drawNet_layers[1]-drawNet_layers[2])/DL_maxlayers;
			drawNet = new Network(drawNet_layers);
			//Play network
			int inputs_p = (USE_PROB_PLAY ? rack_size : rack_size*3) + 1,
				hidden_p = USE_PROB_PLAY ? rack_size*2 : rack_size*4;
			playNet_layers = new int[]{inputs_p, hidden_p, rack_size+1};
			DL_playdelta = (playNet_layers[1]-playNet_layers[2])/DL_maxlayers;
			playNet = new Network(playNet_layers);
		}
	}
	@Override
	public int play() {
		net_play_count++;
		moves_in_round++;
		boolean drawFromDiscard = decideDraw();
		
		int card = game.deck.draw(drawFromDiscard);		
		int slot = decidePlay(rack, card);
		int discard = slot == -1 ? card : rack.swap(card, slot, drawFromDiscard);
		
		//If we think this move was good, we'll keep it
		double newScore = scoreMetric();
		if (newScore-currentScore > 0){
			drawHistory.add(draw_instance);
			playHistory.add(play_instance);
		}
		currentScore = newScore;
		
		return discard;
	}

	private boolean decideDraw(){
		boolean rval;

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
	private int decidePlay(Rack rack, int card) {
		int rval = -1;
		
		createPlayHistory(card);
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
		//System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" ROUND, score = "+score);
		STAT_badmoves += rand_count;
		games_played++;
		saveMoveHistory(won);
		
		//Reset history
		drawHistory.clear();
		playHistory.clear();
	}
	@Override
	public void beginGame(){}
	@Override
	public void scoreGame(boolean won) {
		//System.out.println(playerNumber +": "+(won ? "WON" : "LOST")+" GAME, score = "+score);
	}
	
	private void createDrawHistory(){
		DataInstance ddi = new DataInstance(game.rack_size*3 + 1);
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
		DataInstance pdi = new DataInstance(playNet_layers[0]);
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
		return rack.computeLUS(null) / (double) game.rack_size;
	}
	
	//DEEP LEARNING
	@Override
	public void epoch(){
		super.epoch();
		
		//Deep learning stopping criteria
		//If no improvement, add another deep learning layer
		if (!USE_RAND && DL_layers <= DL_maxlayers && DL_stop.epoch(this)){
			DL_stop.reset();
			resetModel();
			deepLearn();
		}
	}
	private static void deepLearn(){
		DL_layers++;
		//Add another layer
		if (DL_layers < DL_maxlayers){
			int dl = drawNet_layers[1] - DL_layers*DL_drawdelta,
				pl = playNet_layers[1] - DL_layers*DL_playdelta;
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
