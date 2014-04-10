package models;

import interfaces.Model;
import racko.Game;
import racko.Rack;

/**
 * Committee/Ensemble of players
 * Only works for rack size of 5
 * @author isaac
 */
public class ModelEnsemble extends Model{
	private final Model[] ensemble;
	
	public ModelEnsemble() throws Exception{
		ensemble = new Model[]{
			new ModelKyle(false),
			new ModelMax(),
			new ModelBaltar()
		};
	}

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		if (g.rack_size != 5)
			return false;
		for (Model m: ensemble){
			if (!m.register(g, r))
				return false;
		}
		return true;
	}
	@Override
	public boolean decideDraw(int turn) {
		int[] votes = new int[2];
		for (Model m: ensemble)
			votes[m.decideDraw(turn) ? 1 : 0]++;
		return votes[1] > votes[0];
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		int[] votes = new int[game.rack_size+1];
		for (Model m: ensemble)
			votes[m.decidePlay(turn, drawn, fromDiscard)+1]++;
		int max = 0, max_idx = 0;
		for (int i=0; i<votes.length; i++){
			if (votes[i] > max){
				max = votes[i];
				max_idx = i;
			}
		}
		return max_idx-1;
	}
	@Override
	public String toString() {
		return "Ensemble";
	}
	
}
