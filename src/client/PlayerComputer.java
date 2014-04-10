package client;

import interfaces.Model;
import interfaces.Player;
import models.ModelRandom;
import racko.Game;
import racko.Rack;

/**
 * Plays as a computer
 */
public class PlayerComputer extends Player{
	private int turns = 0, no_progress;
	private final Model random, learner;
	
	public PlayerComputer(Model learner){
		super(learner.toString());
		this.learner = learner;
		random = new ModelRandom();
	}

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		no_progress = g.rack_size*2;
		return random.register(g, r) && learner.register(g, r);
	}
	@Override
	public int play(){
		//If we've gone so many moves without progress, do a random move
		//This will hopefully prevent deadlock over a zugzwang situation
		Model m = (++turns % no_progress) == 0 ? random : learner;
		boolean fromDiscard = m.decideDraw(turns);
		int drawn = game.deck.draw(fromDiscard),
			pos = m.decidePlay(turns, drawn, fromDiscard);
		return pos == -1 ? drawn : rack.swap(drawn, pos, fromDiscard);
	}

	@Override
	public void beginRound() {
		turns = 0;
		learner.beginRound();
	}
	@Override
	public void scoreRound(boolean won, int score) {
		learner.scoreRound(won, score);
	}
	@Override
	public void epoch() {
		super.epoch();
		learner.epoch(this);
	}
}
