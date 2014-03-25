package client;

import interfaces.Player;

import java.awt.CardLayout;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import racko.Game;
import racko.Rack;

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
		//testSuite();
		//http://www.spellensite.nl/spellen-spelen.php?type=spellen&spellen=Tower+blaster&id=1291
		
		//*
		//SETTINGS
		int rack_size = 5,			//rack size
			streak_min = 1,			//minimum streak to win
			stats_player = 0,		//which player to show stats for (after each epoch)
			train_games = 3000,		//if play_human = true, how many games to train the AI's beforehand
			play_games = 1,			//how many games to play (after training, if playing a human)
			epoch_every = 150;		//epoch after how many games?
		boolean
			bonus_mode = false,		//use bonus scoring
			play_human = false;		//play against the AI's in a terminal
		
		Player[] players = new Player[]{
			new PlayerAI(true),
			new PlayerAI(false)
		};
				
		//TRAINING & TESTING
		if (play_human){
			//... just testing ... 
			//train the ai first before playing by hand
			System.out.println("Training the AI's, please wait...");
			
			Game train_game = Game.create(players, rack_size, streak_min, bonus_mode);
			for (int i=0; i<train_games; i++){
				train_game.play();
				if (i % 50 == 0)
					System.out.println(i*100/train_games+"% trained");
			}

			System.out.println("\n\n/////// BEGINNING TOURNAMENT ///////");
			Game.verbose = true;
			players = Arrays.copyOf(players, players.length+1);
			players[players.length-1] = new PlayerHuman();
		}
			
		Game g = Game.create(players, rack_size, streak_min, bonus_mode);
		Player pstat = players[stats_player];
		int epochs = 0;		
		for (int i = 0; i < play_games; i++){
			g.play();
			//Print statistics to console
			if (i > 0 && i % epoch_every == 0){
				epochs++;
				System.out.println("Epoch #"+			epochs);
				System.out.println("\tMoves:\t\t"+		GUI.round(pstat.EPOCH_allmoves));
				System.out.println("\tRandom:\t\t"+		GUI.round(pstat.EPOCH_badmoves*100)+"%");
				System.out.println("\tWins:\t\t"+		GUI.round(pstat.EPOCH_wins*100)+"%");
				System.out.println("\tMoves All:\t"+	GUI.round(pstat.MODEL_allmoves));
				System.out.println("\tRandom All:\t"+	GUI.round(pstat.MODEL_badmoves*100)+"%");
				System.out.println("\tWins All:\t"+		GUI.round(pstat.MODEL_wins*100)+"%");
			}
		}
		//*/
	}
	
	private static void testSuite(){
		int[] rack = new int[]{6, 5, 2, 1};
		int size = 5;
		Rack r = new Rack(rack.length, 6);
		r.deal(rack);
		int seq = r.scoreSequence();
		System.out.println(seq);
	}
	private static double round(double val){
		return Math.round(val*100)/100.0;
	}
}
