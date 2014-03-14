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
		
		//*
		//SETUP GAME
		PlayerAI[] players = new PlayerAI[]{
			new PlayerAI(true),
			new PlayerAI(false)
		};
		PlayerAI p_rand = players[0], p_ai = players[1];
		Game g = Game.create(players, 5, 1, false);
		
		//PLAY GAMES
		
		//Statistics setup variables:
		boolean use_DL = true;
		int games = 1000000,
			epoch_every = 150,
			DL_noimprove_max = 25;
		double creep = .04;
		
		//Private counters (don't touch)
		int epochs = 0,
			DL_noimprove = 0;
		boolean[] stats_min = new boolean[]{true, true, false};
		double[] stats_start = new double[]{210, 5, 50};
		double[] stats_cur = Arrays.copyOf(stats_start, 3);
		double[] stats_best = Arrays.copyOf(stats_cur, 3);
		
		for (int i = 0; i < games; i++){
			g.play();
			//Print statistics to console
			if (i > 0 && i % epoch_every == 0){
				epochs++;
				
				//Compute stats
				double[] stats_new = new double[]{
					p_ai.ALL_moves / (double) p_ai.ALL_rounds,
					100 * p_ai.ALL_rand_count/ (double) p_ai.ALL_moves,
					100 * p_ai.ALL_wins / (double) p_ai.ALL_rounds
				};
				for (int j=0; j<stats_cur.length; j++){
					double temp = (1-creep)*stats_cur[j] + creep*stats_new[j];
					if (stats_min[j] && temp < stats_best[j] || !stats_min[j] && temp > stats_best[j]){
						DL_noimprove = 0;
						stats_best[j] = temp;
					}
					stats_cur[j] = temp;
				}
				
				System.out.println("Epoch #"+epochs);
				System.out.println("\tMoves:\t\t"+GUI.round(stats_new[0]));
				System.out.println("\tRandom:\t\t"+GUI.round(stats_new[1])+"%");
				System.out.println("\tWins:\t\t"+GUI.round(stats_new[2])+"%");
				System.out.println("\tMoves All:\t"+GUI.round(stats_best[0]));
				System.out.println("\tRandom All:\t"+GUI.round(stats_best[1])+"%");
				System.out.println("\tWins All:\t"+GUI.round(stats_best[2])+"%");
				
				//Once it stops learning, add another deep learning layer
				if (use_DL && ++DL_noimprove > DL_noimprove_max){
					use_DL = PlayerAI.deepLearn();
					System.arraycopy(stats_start, 0, stats_cur, 0, 3);
					DL_noimprove = 0;
				}
				
				//Reset counters
				for (int j=0; j<players.length; j++){
					players[j].ALL_moves = 0;
					players[j].ALL_rand_count = 0;
					players[j].ALL_rounds = 0;
					players[j].ALL_wins = 0;
				}
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
