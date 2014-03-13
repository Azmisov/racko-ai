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
		Player[] players = new Player[2];
		
		Player p1 = new PlayerRandom();
		Player p2 = new PlayerRandom();
		
		players[0] = p1;
		players[1] = p2;
		
		Game g = Game.create(players, 5, 3, false);
		for(int i = 0; i < 30; i++){
			g.play();
		}
		
		System.out.println("Player AI = "+p1.wins);
		System.out.println("Player Random = "+p2.wins);
	}
}
