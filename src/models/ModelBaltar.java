package models;

import interfaces.Model;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import racko.Game;
import racko.Rack;
import reinforcement.ReinforcementLearner;

/**
 * Ensemble of reinforcement learners
 * @author john
 */
public class ModelBaltar extends Model{
	//game constants
	private Game game;
	private Rack rack;
	//reinforcement learners
	private final ArrayList<ReinforcementLearner> cylons;
	private final ArrayList<Double> weights;
	private final String[] BaltarFiles = new String[]{
		"ReinforcementLearner84647971.txt",
		"ReinforcementLearner129801927.txt",
		"ReinforcementLearner-207920943.txt",
		"ReinforcementLearner261200492.txt",
		"ReinforcementLearner267447822.txt",
		"ReinforcementLearner446609466.txt",
		"ReinforcementLearner-566309831.txt",
		"ReinforcementLearner723045708.txt",
		"ReinforcementLearner-982413113.txt",
		"ReinforcementLearner-1042424524.txt",
		"ReinforcementLearner-1099763312.txt",
		"ReinforcementLearner-1302531921.txt",
		"ReinforcementLearner-1414899564.txt",
		"ReinforcementLearner1462336758.txt",
		"ReinforcementLearner-1581899655.txt",
		"ReinforcementLearner1830453964.txt",
		"ReinforcementLearner1974869635.txt",
		"ReinforcementLearner2068109305.txt",
		"ReinforcementLearner-2090730043.txt",
		"ReinforcementLearner2091629729.txt"
		};
	
	public ModelBaltar(){
		String[] Filenames = BaltarFiles;
		cylons = new ArrayList();
		weights = new ArrayList();
		for (String filename: Filenames)
		{
			try {
				FileReader in = new FileReader(filename);
				BufferedReader buff = new BufferedReader(in);
				weights.add( Double.parseDouble(buff.readLine()));
				cylons.add(new ReinforcementLearner(buff));
			} catch (FileNotFoundException ex) {
				ex.printStackTrace();
			} catch (IOException e){
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void register(Game g, Rack r) throws Exception {
		this.game = g;
		this.rack = r;
		if (g.rack_size != 5)
			System.out.println("Unsupported racksize");
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
		int card = game.deck.draw(fromDiscard);
		
		double[] playVotes = new double[6];
		for (int i=0; i < cylons.size(); i++)
		{
			int vote = cylons.get(i).cardPositionForReal(game, rack, card);
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
		return pos == game.rack_size ? card : rack.swap(card, pos, fromDiscard);
	}
}
