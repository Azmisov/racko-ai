package models;

import interfaces.Player;

/**
 * Stopping criteria for a particular learning model
 * This just goes until there is no improvement for X number of epochs
 * It uses running averages of the players' Player.MODEL_* statistics to
 * detect improvements
 * @author isaac
 */
public class StoppingCriteria {
	//If averages are within EPSILON distance, we treat them as the same value
	public static double EPSILON = 0.00001;
	//Running average creep factor avg = (1-creep)*old + creep*new
	private double creep = .04;
	//If this is the start of a new learning model (e.g. new deep learning layer)
	private boolean new_model = true;
	//How many epochs without improvement before we stop
	private int noimprove_max = 25;
	//How many epochs we've gone without improvement
	private int noimprove;
	//Best scores we've seen thus far (running averages)
	private double max_wins, min_badmoves, min_allmoves;
	
	/**
	 * Create a new stopping criteria with default parameters
	 *  Creep = 0.04
	 *	Epochs = 25
	 */
	public StoppingCriteria(){
		this.creep = 0.04;
		noimprove_max = 25;
	}
	/**
	 * Create stopping criteria with defined parameters
	 * @param creep creep factor for running averages: avg = (1-creep)*old + creep*new;
	 *	default = .04
	 * @param epochs how many epochs to go without improvement before stopping
	 *	default = 25
	 */
	public StoppingCriteria(double creep, int epochs){
		this.creep = creep;
		noimprove_max = epochs;
	}
	
	/**
	 * Signify the end of an epoch
	 * @param p the player to measure for improvement; you may use multiple players
	 * for the same StoppingCriteria object; as long as they're using the same learning model
	 * it should work fine
	 * @return true, if the stopping criteria has been met
	 */
	public boolean epoch(Player p){
		if (noimprove < noimprove_max){
			noimprove++;
			//Update our average EPOCH statistics
			if (!new_model){
				//Update the "noimprove" and DL_max/DL_min variables
				if (p.MODEL_allmoves-min_allmoves+EPSILON < 0){
					min_allmoves = p.MODEL_allmoves;
					noimprove = 0;
				}
				if (p.MODEL_badmoves-min_badmoves+EPSILON < 0){
					min_badmoves = p.MODEL_badmoves;
					noimprove = 0;
				}
				if (p.MODEL_wins-max_wins-EPSILON > 0){
					max_wins = p.MODEL_wins;
					noimprove = 0;
				}
			}
			//If it is the start of an epoch, just set averages to current
			//In most situations, this should work fine, since accuracy is not at it's peak at the start
			else{
				new_model = false;
				min_allmoves = p.MODEL_allmoves;
				min_badmoves = p.MODEL_badmoves;
				max_wins = p.MODEL_wins;
			}
		}
		return noimprove >= noimprove_max;
	}
	/**
	 * Reset the stopping criteria
	 * Use this if the learning model changes (e.g. you add
	 * another hidden layer to a deep neural network)
	 * Make sure to also call Player.resetModel() as well!!!
	 */
	public void reset(){
		new_model = true;
		noimprove = 0;
	}
}
