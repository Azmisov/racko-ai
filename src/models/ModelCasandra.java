package models;

import NeuralNetworks.Network;
import interfaces.Model;
import java.io.File;
import racko.DataInstance;
import racko.Game;
import racko.Rack;

/**
 * Mimics another player
 * @author chris
 */
public class ModelCasandra extends Model{
	//Neural Network
	private final boolean USE_PROB_DRAW = true, USE_PROB_PLAY = false;
	private Network drawNet = null, playNet = null;
	//Deep learning
	private final int DL_maxlayers = 4;
	private int DL_drawdelta, DL_playdelta, DL_layers = 0, rack_size;
	private final StoppingCriteria DL_stop = new StoppingCriteria();
	
	/**
	 * Loads a Casandra from file or trains a new one
	 * @param draw_file file to load/save to (for drawing cards); if null or doesn't exist, will train anew
	 * @param play_file same as draw_file, except for placing drawn cards	
	 * @param rack_size the rack size to train against
	 * @throws Exception if model is null and can't load network; rack_size
	 * doesn't match the one of the loaded network
	 */
	public ModelCasandra(String draw_file, String play_file, int rack_size) throws Exception{
		boolean loaded = false;
		if (draw_file != null){
			File f = new File(draw_file);
			if (f.isFile()){
				try{
					
				} catch (Exception e){
					System.out.println("Warning! Could not load Casandra weights!");
				}
			}
		}
	}
	/**
	 * Copies the specified model
	 * @param net model to copy
	 */
	public ModelCasandra(ModelCasandra net){
		
	}
	
	/**
	 * Train to mimic this model
	 * @param m 
	 */
	public void train(Model m){
		
	}

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		return g.rack_size == rack_size;
	}
	@Override
	public boolean decideDraw(int turn) {
		DataInstance drawInstance = createDrawHistory();
		drawNet.compute(drawInstance.inputs);
		boolean drawFromDiscard = drawNet.getOutput() > .5;
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}