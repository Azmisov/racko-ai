package models;

import interfaces.Model;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import racko.Game;
import racko.Rack;
import reinforcement.ReinforcementLearner;

/**
 * Ensemble of reinforcement learners
 * @author john
 */
public class ModelBaltar extends Model{
	//reinforcement learners
	private final ArrayList<ReinforcementLearner> cylons;
	private final ArrayList<Double> weights;
	private final String[] BaltarFiles = new String[]{
		"weights/baltar/ReinforcementLearner84647971.txt",
		"weights/baltar/ReinforcementLearner129801927.txt",
		"weights/baltar/ReinforcementLearner-207920943.txt",
		"weights/baltar/ReinforcementLearner261200492.txt",
		"weights/baltar/ReinforcementLearner267447822.txt",
		"weights/baltar/ReinforcementLearner446609466.txt",
		"weights/baltar/ReinforcementLearner-566309831.txt",
		"weights/baltar/ReinforcementLearner723045708.txt",
		"weights/baltar/ReinforcementLearner-982413113.txt",
		"weights/baltar/ReinforcementLearner-1042424524.txt",
		"weights/baltar/ReinforcementLearner-1099763312.txt",
		"weights/baltar/ReinforcementLearner-1302531921.txt",
		"weights/baltar/ReinforcementLearner-1414899564.txt",
		"weights/baltar/ReinforcementLearner1462336758.txt",
		"weights/baltar/ReinforcementLearner-1581899655.txt",
		"weights/baltar/ReinforcementLearner1830453964.txt",
		"weights/baltar/ReinforcementLearner1974869635.txt",
		"weights/baltar/ReinforcementLearner2068109305.txt",
		"weights/baltar/ReinforcementLearner-2090730043.txt",
		"weights/baltar/ReinforcementLearner2091629729.txt"
	};
	
	public ModelBaltar() throws Exception{
		String[] Filenames = BaltarFiles;
		cylons = new ArrayList();
		weights = new ArrayList();
		for (String filename: Filenames){
			FileReader in = new FileReader(filename);
			BufferedReader buff = new BufferedReader(in);
			weights.add( Double.parseDouble(buff.readLine()));
			cylons.add(new ReinforcementLearner(buff));
		}
	}

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		return g.rack_size == 5;
	}
	@Override
	public boolean decideDraw(int turn) {
		double drawVote = 0;
		for (int i=0; i < cylons.size(); i++){
			if (cylons.get(i).fromDiscardForReal(game, rack))
				drawVote += weights.get(i);
			else
				drawVote -= weights.get(i);
		}
		return drawVote > 0;
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		
		double[] playVotes = new double[6];
		for (int i=0; i < cylons.size(); i++)
		{
			int vote = cylons.get(i).cardPositionForReal(game, rack, drawn);
			playVotes[vote] += weights.get(i);
		}
		double bestVote = 0;
		int pos = 0;
		for (int i=0; i < playVotes.length; i++)
		{
			if (playVotes[i] > bestVote)
			{
				bestVote = playVotes[i];
				pos = i;
			}
		}
		return pos == game.rack_size ? -1 : pos;
	}

	@Override
	public String toString() {
		return "Baltar";
	}
}
