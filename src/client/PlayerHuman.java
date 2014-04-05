package client;

import interfaces.Player;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as a human
 */
public class PlayerHuman extends Player{
	private double max_points;

	public PlayerHuman(){
		super();
	}

	@Override
	public void register(Game g, Rack r) {
		super.register(g, r);
		max_points = g.maxPoints();
	}
	
	@Override
	public int play(){
		System.out.println("Discard: "+game.deck.peek(true)+", Rack: "+rack.toString());
		char draw;
		do{
			String output = System.console().readLine("Draw from discard [y/n]:");
			draw = output.toLowerCase().charAt(0);
		} while(draw != 'n' && draw != 'y');
		boolean fromDiscard = draw == 'y';
		int drawn = game.deck.draw(fromDiscard);
		int discard = -1;
		do{
			String pos = System.console().readLine("Drew %d, Discard:", drawn);
			try{
				discard = Integer.parseInt(pos);
			} catch (Exception e){}
		} while (discard == -1 || (discard != drawn && !rack.contains(discard)));
		if (discard != drawn){
			int[] cards = rack.getCards();
			for (int i=0; i<cards.length; i++){
				if (cards[i] == discard)
					rack.swap(drawn, i, fromDiscard);
			}
		}
		return discard;
	}

	@Override
	public void scoreRound(boolean won, int score) {
		if (won)
			System.out.println("You won the round!\n");
		else System.out.println("You lost the round.\n");
	}

	@Override
	public void scoreGame(boolean won) {
		if (won)
			System.out.println("You won the game!\n");
		else System.out.println("You lost the game.\n");
	}
	
}
