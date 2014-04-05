package client;

import java.util.ArrayList;
import java.util.Random;

import racko.DataInstance;
import racko.Game;
import racko.Rack;
import interfaces.Player;

public class PlayerCasandra extends Player{

	private static final Random RAND = new Random();
	//Playing history
	private final ArrayList<DataInstance> drawHistory;
	private final ArrayList<DataInstance> playHistory;
	private DataInstance draw_instance, play_instance;
	
	public PlayerCasandra()
	{
		super();
		drawHistory = new ArrayList();
		playHistory = new ArrayList();
	}
	
	public void register(Game g, Rack r) {
		super.register(g, r);
		
		Player[] players = {new PlayerKyle(true), new PlayerKyle(true)};
		PlayerKyle.save_moves = true;
		
		Game g2 = Game.create(players, g.rack_size, g.min_streak, g.bonus_mode);
		g2.play(0);
		
		ArrayList<DataInstance> kyle_moves = PlayerKyle.play_history;
		
		PlayerKyle.save_moves = false;
		
		//do stuff here
	}
	
	@Override
	public int play() {
		
		
		
		return 0;
	}

}
