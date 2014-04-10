package models;

import NeuralNetworks.Network;
import client.PlayerComputer;
import interfaces.Model;
import interfaces.Player;
import java.io.File;
import java.util.Random;
import racko.Game;
import racko.Rack;

/**
 * Mimics another player
 * @author chris
 */
public class ModelCasandra extends Model{
	private static final Random RAND = new Random();
	//Neural Network
	private final Model model_mimic;
	private final boolean USE_PROB_DRAW = false, USE_PROB_PLAY = false;
	public Network drawNet, playNet;
	private String drawNet_file, playNet_file;
	//Deep learning
	private final int DL_maxlayers = 4, rack_size;
	private int DL_drawdelta, DL_playdelta, DL_layers = 0;
	private final StoppingCriteria DL_stop = new StoppingCriteria(.04, 100);
	
	/**
	 * Loads a Casandra from file or trains a new one
	 * @param draw_file file to load/save to (for drawing cards); if null or doesn't exist, will train anew
	 * @param play_file same as draw_file, except for placing drawn cards	
	 * @param m	model to mimic; null to disable training
	 * @param rack_size the rack size to train against
	 * @throws Exception rack_size doesn't match the one of the loaded network
	 */
	public ModelCasandra(String draw_file, String play_file, Model m, int rack_size) throws Exception{
		this.rack_size = rack_size;
		model_mimic = m;
		if (!loadNetwork(draw_file, true) || !loadNetwork(play_file, false))
			throw new Exception("Invalid network configuration for Casandra");
	}
	/**
	 * Copies the specified model, using a predefined network
	 * @param copy model to copy data from
	 * @param mimic model to train against (null, to not train)
	 */
	public ModelCasandra(ModelCasandra copy, Model mimic){
		rack_size = copy.rack_size;
		drawNet = copy.drawNet;
		playNet = copy.playNet;
		drawNet_file = copy.drawNet_file;
		playNet_file = copy.playNet_file;
		DL_drawdelta = copy.DL_drawdelta;
		DL_playdelta = copy.DL_playdelta;
		DL_layers = copy.DL_layers;
		model_mimic = mimic;
	}
	private boolean loadNetwork(String file, boolean forDraw){
		boolean loaded = false;
		Network net = null;
		//Try to load from a file
		if (file != null){
			File f = new File(file);
			if (f.isFile()){
				try{
					net = new Network(file);
					loaded = true;
				} catch (Exception e){
					System.out.println("Warning! Could not load Casandra weights!");
				}
			}
		}
		//Otherwise, create a new network
		if (!loaded){
			int inputs = nodeCount(forDraw, true);
			net = new Network(new int[]{inputs, forDraw ? inputs*2 : inputs*3, nodeCount(forDraw, false)});
		}
		//Validate network size
		else if (net.inputNodes() != nodeCount(forDraw, true) || net.outputNodes() != nodeCount(forDraw, false))
			return false;
		//Save network
		if (forDraw){
			drawNet = net;
			drawNet_file = file;
		}
		else{
			playNet = net;
			playNet_file = file;
		}
		return true;
	}

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		return model_mimic.register(g, r) && g.rack_size == rack_size;
	}
	@Override
	public boolean decideDraw(int turn){
		DataInstance d = getHistory(game.deck.peek(true), true);
		drawNet.compute(d.inputs);
		boolean actual = drawNet.getOutput() > .5;
		//Train
		if (model_mimic != null){
			boolean target = model_mimic.decideDraw(turn);
			drawNet.trainBackprop(0.01, target ? 1 : 0);
		}
		return actual;
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		DataInstance d = getHistory(drawn, false);
		playNet.compute(d.inputs);
		int actual = playNet.getOutput()-1;
		//Train
		if (model_mimic != null){
			int target = model_mimic.decidePlay(turn, drawn, fromDiscard);
			playNet.trainBackprop(0.01, target+1);
		}
		return actual;
	}
	
	/**
	 * How many nodes in a layer
	 * @param forDraw draw network (true) or play network (false)
	 * @param input input layer (true) or output layer (false)
	 * @return nodes in this layer
	 */
	private int nodeCount(boolean forDraw, boolean input){
		if (input){
			boolean use_prob = forDraw ? USE_PROB_DRAW : USE_PROB_PLAY;
			return rack_size + (use_prob ? rack_size*2 : 0) + 1;
		}
		return forDraw ? 1 : rack_size + 1;
	}
	/**
	 * Get features for this rack
	 * @param card if forDraw == true, top of discard; otherwise, the card that was drawn
	 * @param forDraw features for drawNet? false, for playNet
	 * @return data instance with the feature values
	 */
	private DataInstance getHistory(int card, boolean forDraw){
		boolean use_prob = forDraw ? USE_PROB_DRAW : USE_PROB_PLAY;
		DataInstance d = new DataInstance(nodeCount(forDraw, true));

		//Rack
		d.addFeature(rack.getCards(), game.card_count);
		//Probabilities
		if (use_prob){
			double[][] prob = rack.getProbabilities(false, 0);
			d.addFeature(prob[0], 1);
			d.addFeature(prob[1], 1);
		}
		//The card that was drawn
		d.addFeature(card, game.card_count);

		return d;
	}
	
	//DEEP LEARNING
	@Override
	public void epoch(Player p) {
		//Save the network
		if (model_mimic != null){
			if (drawNet_file != null)
				drawNet.export(drawNet_file);
			if (playNet_file != null)
				playNet.export(playNet_file);

			//Deep learning stopping criteria
			//If no improvement, add another deep learning layer
			if (DL_layers <= DL_maxlayers && DL_stop.epoch(p)){
				DL_stop.reset();
				p.resetModel();
				deepLearn();
			}
		}
	}
	private void deepLearn(){
		DL_layers++;
		//Add another layer
		if (DL_layers < DL_maxlayers){
			int dl = drawNet.layers.get(1).length - DL_layers*DL_drawdelta,
				pl = playNet.layers.get(1).length - DL_layers*DL_playdelta;
			//if (Game.verbose)
				System.out.println("Casandra: Adding DEEP LEARNING layer #"+DL_layers+" ("+dl+", "+pl+" nodes)");
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
				System.out.println("Casandra: Beginning DEEP LEARNING refinement stage");
		}
	}

	@Override
	public String toString() {
		return "Casandra";
	}
}