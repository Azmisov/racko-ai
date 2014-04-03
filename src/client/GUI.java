package client;

import interfaces.Player;

import java.awt.CardLayout;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import racko.Game;
import racko.Rack;
import reinforcement.*;

/**
 * Human usable interface for playing racko
 */
public class GUI extends JFrame{
	private static final Random rand = new Random();
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
		//testSuite();
		//http://www.spellensite.nl/spellen-spelen.php?type=spellen&spellen=Tower+blaster&id=1291
		
		//*
		//SETTINGS
		int rack_size = 5,			//rack size
			streak_min = 1,			//minimum streak to win
			train_games = 0,		//if play_human = true, how many games to train the AI's beforehand
			play_games = 1000000,			//how many games to play (after training, if playing a human)
			epoch_every = 2000;		//epoch after how many games?
		boolean
			bonus_mode = false,		//use bonus scoring
			play_human = false;		//play against the AI's in a terminal
		
		Player[] players = new Player[]{
			//new PlayerAI(false),
			//new PlayerAI(true),
			//new PlayerMax(),
			//new PlayerKyle(true)
			new PlayerRandom(),
			new PlayerExploiter()
		};

		//TRAINING & TESTING
		if (play_human){
			//... just testing ... 
			//train the ai first before playing by hand
			System.out.println("Training the AI's, please wait...");
			
			Game train_game = Game.create(players, rack_size, streak_min, bonus_mode);
			for (int i=0; i<train_games; i++){
				train_game.play(rand.nextInt(players.length));
				if (i % 50 == 0)
					System.out.println(i*100/train_games+"% trained");
			}

			System.out.println("\n\n/////// BEGINNING TOURNAMENT ///////");
			Game.verbose = true;
			players = Arrays.copyOf(players, players.length+1);
			players[players.length-1] = new PlayerHuman();
		}
			
		Game g = Game.create(players, rack_size, streak_min, bonus_mode);		
		int epochs = 0;
		for (int i = 0; i < play_games; i++){
			g.play(rand.nextInt(players.length));
			if (i > 0 && i % epoch_every == 0){
				epochs++;
				//Notify players of epoch
				for (Player p: players)
					p.epoch();
				
				//Print statistics to console
				System.out.println("EPOCH #"+epochs+":");
				System.out.println(
					"\tMoves:\t\t"+
					"Random:\t\t"+
					"Wins:\t\t"+
					"Moves All:\t"+
					"Random All:\t"+
					"Wins All:"
				);
				for (Player p: players){
					System.out.println(
						"P"+p.playerNumber+"\t"+
						GUI.round(p.EPOCH_allmoves)+"\t\t"+
						GUI.round(p.EPOCH_badmoves*100)+"%\t\t"+
						GUI.round(p.EPOCH_wins*100)+"%\t\t"+
						GUI.round(p.MODEL_allmoves)+"\t\t"+
						GUI.round(p.MODEL_badmoves*100)+"%\t\t"+
						GUI.round(p.MODEL_wins*100)+"%"
					);
				}
			}
		}
		//*/
	}
	
	private static void testSuite(){
		
	}
	private static double round(double val){
		return Math.round(val*100)/100.0;
	}
}
