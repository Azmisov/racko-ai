package models;

import interfaces.Model;
import java.util.Random;
import racko.Game;
import racko.Rack;

/**
 * Plays random moves
 */
public class ModelRandom extends Model{
	private static final Random RAND = new Random();
	private int rack_size;
	
	@Override
	public boolean register(Game g, Rack r){
		super.register(g, r);
		rack_size = g.rack_size;
		return true;
	}
	@Override
	public boolean decideDraw(int turn) {
		return RAND.nextBoolean();
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		return RAND.nextInt(rack_size+1)-1;
	}

	@Override
	public String toString() {
		return "Random";
	}
}
