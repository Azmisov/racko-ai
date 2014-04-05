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
	public void register(Game g, Rack r) {
		rack_size = g.rack_size;
	}
	@Override
	public boolean decideDraw(int turn) {
		return RAND.nextBoolean();
	}
	@Override
	public int decidePlay(int turn, int drawn, boolean fromDiscard) {
		return RAND.nextInt(rack_size+1)-1;
	}
}
