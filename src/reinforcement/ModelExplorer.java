/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reinforcement;
import interfaces.*;
import client.*;
import java.util.Random;
import models.ModelRandom;
import racko.Game;
import racko.Rack;

public class ModelExplorer extends Model{
	private Game game;
	private Rack rack;
	private ReinforcementLearner rl = new ReinforcementLearner();

	@Override
	public void register(Game g, Rack r) throws Exception {
		this.game = g;
		this.rack = r;
	}
	@Override
	public boolean decideDraw(int turn) {
		return rl.fromDiscard(game, rack);
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		return rl.cardPosition(game, rack, drawn);
	}
	
	@Override
	public void scoreRound(boolean won, int score){
		super.scoreRound(won, score);
		rl.gameEnd(won);
	}
	
	public void train(){
		int sinceBelowMin = 0;
		Player[] players = new Player[]{
			new PlayerComputer(this),
			new PlayerComputer(new ModelRandom())
		};
		while (sinceBelowMin < 50)
		{
			Game g = Game.create(players, 5, 1, false);
			g.play(new Random().nextInt(players.length));
			sinceBelowMin = rl.gamesSinceBelowMin();
		}
	}
	public ReinforcementLearner getRL(){
		return rl;
	}
}
