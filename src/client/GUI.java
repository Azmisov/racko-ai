package client;

import interfaces.Player;

import java.awt.CardLayout;

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
		
		//*
		//Setup the game
		PlayerAI[] players = new PlayerAI[]{
			new PlayerAI(true),
			new PlayerAI(false)
		};
		PlayerAI p_rand = players[0], p_ai = players[1];
		Game g = Game.create(players, 5, 3, false);
		
		//Play games
		System.out.println("Note: epoch statistics are only snapshots, not averages");
		int epoch_every = 100, epochs = 0, games = 1000000, no_improve = 0;
		double round = 100, wins_ALL = -1, rand_ALL = -1, wins_ALL_MAX = 0;
		for (int i = 0; i < games; i++){
			g.play();
			//Print statistics to console
			if (i > 0 && i % epoch_every == 0){
				epochs++;
				double wins = 100 * p_ai.wins / (double) (p_rand.wins + p_ai.wins),
						score = 100 * p_ai.score / (double) (p_rand.score + p_ai.score),
						rand = 100 * p_ai.rand_count/(double) p_ai.movesInGame;
				int roundMoves = p_ai.movesInGame / (p_rand.wins + p_ai.wins);
				
				System.out.println("Epoch #"+epochs);
				System.out.println("\tMoves per round = "+roundMoves);
				System.out.println("\tRandom moves = "+(Math.round(rand*round)/round)+"%");
				System.out.println("\tAI wins = "+(Math.round(wins*round)/round)+"%");
				//System.out.println("\tAI score = "+(Math.round(score*round)/round)+"%");
				
				//Reset counters
				for (int j=0; j<players.length; j++){
					players[j].wins = 0;
					players[j].score = 0;
					players[j].rand_count = 0;
				}
				
				//Once it stops learning, add another deep learning layer
				if (wins_ALL == -1){
					wins_ALL = wins;
					rand_ALL = rand;
				}
				else{
					wins_ALL = .96*wins_ALL + .04*wins;
					rand_ALL = .96*rand_ALL + .04*rand;
				}
				if (wins_ALL > wins_ALL_MAX){
					wins_ALL_MAX = wins_ALL;
					no_improve = 0;
				}
				else if (++no_improve > 5){
					PlayerAI.deepLearn();
					no_improve = 0;
				}
				
				System.out.println("\tOverall random moves = "+(Math.round(rand_ALL*round)/round)+"%");
				System.out.println("\tOverall wins = "+(Math.round(wins_ALL*round)/round)+"%");
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
}
