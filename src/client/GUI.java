package client;

import interfaces.Player;

import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import racko.Game;

/**
 * Human usable interface for playing racko
 */
public class GUI extends JFrame{
	//Window title
	private static final String title = "Racko v0.1";
	//Window dimensions
	private static final int width = 750, height = 700;
	//GUI items
	CardLayout views;
	
	private GUI(){
		initGUI();
	}
	
	//GUI Initialization
	private void initGUI(){
		//Switches between GUI views (e.g. game mode / settings mode)
		views = new CardLayout();
	}
	private JScrollPane initSettingsView(){
		/**
		 * Game settings: 
		 * - rack size
		 * - min streak
		 * - bonus mode
		 * - players
		 *		- selection box for each player
		 * - scoring
		 *		- score_win
		 *		- score_all
		 *		- score_single
		 *		- score_bonus
		 *		- score_bonus_fac
		 *		- bonus_min
		 *		- bonus_max
		 */
		JScrollPane scroll = new JScrollPane();
		return scroll;
	}
	private void initGameView(){
		
	}
	
	public static void main(String[] args){
		//Setup the game
		Player[] players = new Player[]{
			new PlayerRandom(),
			new PlayerAI()
		};
		Player p_rand = players[0], p_ai = players[1];
		Game g = Game.create(players, 5, 3, false);
		
		//Play games
		double round = 100;
		int epoch_every = 10, epochs = 0;
		for (int i = 0; i < 500; i++){
			g.play();
			//Print statistics to console
			if (i % epoch_every == 0){
				epochs++;
				System.out.println("Epoch #"+epochs);
				System.out.println("\t"+p_ai.wins+" to "+p_rand.wins);
				double wins = 100 * p_ai.wins / (double) (p_rand.wins + p_ai.wins),
						score = 100 * p_ai.score / (double) (p_rand.score + p_ai.score);
				for (int j=0; j<players.length; j++){
					players[j].wins = 0;
					players[j].score = 0;
				}
				System.out.println("\tAI wins = "+(Math.round(wins*round)/round)+"%");
				System.out.println("\tAI score = "+(Math.round(score*round)/round)+"%");
			}
		}
	}
}
