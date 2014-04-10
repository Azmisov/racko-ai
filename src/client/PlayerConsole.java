package client;

import interfaces.Model;
import interfaces.Player;
import models.*;
import racko.Game;
import racko.Rack;

/**
 * Plays the game as a human
 */
public class PlayerConsole extends Player{
	private double max_points;
	private Model[] hinters;

	public PlayerConsole(){
		super();
		hinters = new Model[]{
			new ModelDiablo("weights/diablo/diablo2_weights.txt", false),
			new ModelMax(),
			new ModelKyle(false)
		};
	}

	@Override
	public boolean register(Game g, Rack r) {
		super.register(g, r);
		max_points = g.maxPoints();
		try {
			for (Model m: hinters)
				m.register(g, r);
		} catch (Exception ex) {
			System.out.println("Could not enable hinting!!!");
		}
		return true;
	}
	
	@Override
	public int play(){
		System.out.println("Discard: "+game.deck.peek(true)+", Rack: "+rack.toString());
		char draw;
		String hint_draw = "";
		for (Model m: hinters)
			hint_draw += m.decideDraw(0) ? "y" : "n";
		do{
			String output = System.console().readLine("Draw from discard [y/n/h]:");
			draw = output.toLowerCase().charAt(0);
			if (draw == 'h')
				System.out.println("(Hint: "+hint_draw+")");
		} while(draw != 'n' && draw != 'y');
		boolean fromDiscard = draw == 'y';
		int drawn = game.deck.draw(fromDiscard);
		int discard = -1;
		String hint_play = "";
		for (Model m: hinters){
			int hint_pos = m.decidePlay(0, drawn, fromDiscard);
			int hint_discard = hint_pos == -1 ? drawn : rack.getCardAt(hint_pos);	
			hint_play += hint_discard+", ";
		}
		do{
			String pos = System.console().readLine("Drew %d, Discard:", drawn);
			if (pos.equals("h"))
				System.out.println("(Hint: "+hint_play+")");
			else{
				try{
					discard = Integer.parseInt(pos);
				} catch (Exception e){}
			}
		} while (discard == -1 || (discard != drawn && !rack.contains(discard)));
		if (discard != drawn){
			int[] cards = rack.getCards();
			for (int i=0; i<cards.length; i++){
				if (cards[i] == discard)
					rack.swap(drawn, i, fromDiscard);
			}
		}
		System.out.println();
		return discard;
	}

	@Override
	public void scoreRound(boolean won, int score) {
		if (won)
			System.out.println("You won the round!\n");
		else System.out.println("You lost the round.\n");
	}

	@Override
	public void scoreGame(boolean won, int score) {
		if (won)
			System.out.println("You won the game!\n");
		else System.out.println("You lost the game.\n");
	}
	
}
