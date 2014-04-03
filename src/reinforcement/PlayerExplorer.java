/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package reinforcement;
import interfaces.*;
import client.*;
import java.util.Random;
import racko.Game;

public class PlayerExplorer extends Player{
	private ReinforcementLearner rl = new ReinforcementLearner();
	
	@Override
	public int play()
	{
		boolean fromDiscard = rl.fromDiscard(this);
		int card = game.deck.draw(fromDiscard);
		int pos = rl.cardPosition(this, card);
		return pos == game.rack_size ? card : rack.swap(card, pos, fromDiscard);
	}
	
	@Override
	public void scoreRound(boolean won, int score)
	{
		super.scoreRound(won, score);
		rl.gameEnd(won);
	}
	
	public void train()
	{
		int sinceBelowMin = 0;
		Player[] players = new Player[]{this, new PlayerRandom()};
		while (sinceBelowMin < 50)
		{
			Game g = Game.create(players, 5, 1, false);
			g.play(new Random().nextInt(players.length));
			sinceBelowMin = rl.gamesSinceBelowMin();
		}
	}
	public ReinforcementLearner getRL()
	{
		return rl;
	}
}
