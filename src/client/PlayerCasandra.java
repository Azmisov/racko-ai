package client;

import java.util.ArrayList;
import java.util.Random;

import NeuralNetworks.Network;
import racko.DataInstance;
import racko.Game;
import racko.Rack;
import interfaces.Player;

public class PlayerCasandra extends Player{

	private static final Random RAND = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	
	//Neural Network
	private boolean USE_PROB_DRAW = true;
	private boolean USE_PROB_PLAY = false;
	private static int[] drawNet_layers, playNet_layers;
	private static Network drawNet = null, playNet = null;
	//Deep learning
	private static final int DL_maxlayers = 4;
	private static int DL_drawdelta, DL_playdelta, DL_layers = 0, rack_size;
	private static final StoppingCriteria DL_stop = new StoppingCriteria();
	
	public PlayerCasandra()
	{
		super();
		drawHistory = new ArrayList<DataInstance>();
		playHistory = new ArrayList<DataInstance>();
	}
	
	public void register(Game g, Rack r) {
		super.register(g, r);
		
		Player[] players = {new PlayerKyle(true), new PlayerKyle(true)};
		PlayerKyle.save_moves = true;
		
		Game g2 = Game.create(players, g.rack_size, g.min_streak, g.bonus_mode);
		for(int i = 0; i < 5; i++){
			g2.play(0);
		}
		
		ArrayList<DataInstance> kyle_moves = PlayerKyle.play_history;
		ArrayList<DataInstance> kyle_draws = PlayerKyle.draw_history;
		
		PlayerKyle.save_moves = false;
		
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
		
		double rate = 0.01;
		
		for (DataInstance d: kyle_draws){
			drawNet.compute(d.inputs);
			drawNet.trainBackprop(rate, (int) d.output);
		}
		//Train for playing
		for (DataInstance p: kyle_moves){			
			playNet.compute(p.inputs);
			playNet.trainBackprop(rate, (int) p.output);
		}
	}
	
	@Override
	public int play() {
		
		DataInstance drawInstance = createDrawHistory();
		drawNet.compute(drawInstance.inputs);
		boolean drawFromDiscard = drawNet.getOutput() > .5;
		drawInstance.setOutput(drawFromDiscard);
		
		int cardDrawn = game.deck.draw(drawFromDiscard);
		
		DataInstance playInstance = createPlayHistory(cardDrawn);
		playNet.compute(playInstance.inputs);
		int slot = playNet.getOutput()-1;
		playInstance.setOutput(slot+1,1);
		
		drawHistory.add(drawInstance);
		playHistory.add(playInstance);
		
		int discard = slot == -1 ? cardDrawn : rack.swap(cardDrawn, slot, drawFromDiscard);	
		
		return discard;
	}
	
	public void scoreRound(boolean won, int score){
		if(won){
			
			double rate = 0.01;
			
			for(DataInstance d: drawHistory){
				drawNet.compute(d.inputs);
				drawNet.trainBackprop(rate, (int) d.output);
			}
			
			for (DataInstance p: playHistory){			
				playNet.compute(p.inputs);
				playNet.trainBackprop(rate, (int) p.output);
			}
		}
		
		drawHistory.clear();
		playHistory.clear();
	}
	
	private DataInstance createDrawHistory(){
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
		
		return ddi;
	}
	private DataInstance createPlayHistory(int drawnCard){
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
		return pdi;
	}

}
