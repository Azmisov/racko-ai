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
	private static final double LEARN_RATE = .15;
	private static final Random RAND = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	private DataInstance draw_instance, play_instance;
	//Learning model
	private final boolean use_random;
	private static final int RAND_LIMIT = 20, RAND_ROUNDS = 0;
	private static final int[]
		drawNet_layers = new int[]{16, 32, 1},
		playNet_layers = new int[]{6, 32, 6};
	private static final Network
		drawNet = new Network(drawNet_layers),
		playNet = new Network(playNet_layers);
	//Deep learning
	private static final int
		DL_maxlayers = 10,
		DL_drawdelta = (drawNet_layers[1]-drawNet_layers[2])/DL_maxlayers,
		DL_playdelta = (playNet_layers[1]-playNet_layers[2])/DL_maxlayers;
	private static int DL_layers = 0;
	//Stopping criteria for a deep learning layer
	private static final int DL_noimprove_max = 25;
	private static final double DL_noimprove_creep = .04;
	private static boolean DL_layer_start = true;
	private static int DL_noimprove;
	private static double
		DL_max_wins, DL_min_badmoves, DL_min_allmoves;
	//Statistics
	private double initialScore, currentScore;
	private int games_played = 0, net_play_count, moves_in_round;
	public int rand_count = 0;
	
	public PlayerAI(boolean random){
		super();
		use_random = random;
		drawHistory = new ArrayList();
		playHistory = new ArrayList();
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
		
		if (Game.verbose)
			System.out.println("\tAI"+(use_random ? "-rand" : "-net")+": "+rack.toString());
		
		return discard;
	}

	private boolean decideDraw(){
		boolean rval;

		//We only add the draw instnace if the decidePlay outcome is good
		createDrawHistory();
		if (!use_random && net_play_count < RAND_LIMIT && games_played > RAND_ROUNDS){
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
		if (!use_random && net_play_count < RAND_LIMIT && games_played > RAND_ROUNDS){
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
		
		draw_instance = ddi;
	}
	private void createPlayHistory(int drawnCard){
		//Create the history
		DataInstance pdi = new DataInstance(game.rack_size + 1);
		pdi.addFeature(rack.getCards(), game.card_count);
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
		
		if (rate > .00001){
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
		return rack.scoreSequence() / (double) game.rack_size;
	}
	
	//DEEP LEARNING
	@Override
	public void epoch(){
		super.epoch();
		
		//Deep learning stopping criteria
		if (DL_layers != DL_maxlayers){
			//Update our average EPOCH statistics
			if (!DL_layer_start){				
				//Update the "noimprove" and DL_max/DL_min variables
				if (MODEL_allmoves < DL_min_allmoves){
					DL_min_allmoves = MODEL_allmoves;
					DL_noimprove = 0;
				}
				if (MODEL_badmoves < DL_min_badmoves){
					DL_min_badmoves = MODEL_badmoves;
					DL_noimprove = 0;
				}
				if (MODEL_wins > DL_max_wins){
					DL_max_wins = MODEL_wins;
					DL_noimprove = 0;
				}
			}
			//If it is the start of an epoch, just set averages to current
			//In most situations, this should work fine, since accuracy is not at it's peak at the start
			else{
				DL_layer_start = false;
				DL_min_allmoves = MODEL_allmoves;
				DL_min_badmoves = MODEL_badmoves;
				DL_max_wins = MODEL_wins;
			}

			//If no improvement, add another deep learning layer
			if (DL_noimprove == DL_noimprove_max){
				deepLearn();
				resetModel();
				DL_layer_start = true;
				DL_noimprove = 0;
			}
		}
	}
	private static boolean deepLearn(){
		if (DL_layers < DL_maxlayers){
			DL_layers++;
			int dl = drawNet_layers[1] - DL_layers*DL_drawdelta,
				pl = playNet_layers[1] - DL_layers*DL_playdelta;
			if (Game.verbose)
				System.out.println("Adding DEEP LEARNING layer #"+DL_layers+" ("+dl+", "+pl+" nodes)");
			drawNet.addHiddenLayer(dl);
			playNet.addHiddenLayer(pl);
			drawNet.freeze(DL_layers);
			playNet.freeze(DL_layers);
			return true;
		}
		//Unfreeze all layers
		else if (DL_layers == DL_maxlayers){
			drawNet.freeze(0);
			playNet.freeze(0);
			if (Game.verbose)
				System.out.println("Beginning DEEP LEARNING refinement stage");
		}
		return false;
	}
}
