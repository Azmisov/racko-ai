package client;

import interfaces.Model;
import interfaces.Player;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import models.*;
import racko.Game;
import racko.Rack;

/**
 * Test the AI's
 * @author isaac
 */
public class Testing {
	private static final Random rand = new Random();
	public static void main(String[] args){
		//testSuite();
		//if (true) return;
		//http://www.spellensite.nl/spellen-spelen.php?type=spellen&spellen=Tower+blaster&id=1291
		
		/**
		 * TESTING NEEDED:
		 * - AI			BUGS
		 * - Casandra
		 * - TD			BUGS
		 */
		
		//*
		//SETTINGS
		int rack_size = 10,			//rack size
			streak_min = 1,			//minimum streak to win
			train_games = 0,		//if play_human = true, how many games to train the AI's beforehand
			play_games = 1000000,	//how many games to play (after training, if playing a human)
			epoch_every = 100,		//epoch after how many games?
			move_limit = 5000;		//moves before calling a draw (0 for unlimited)
		boolean
			bonus_mode = false,		//use bonus scoring
			play_human = true;		//play against the AI's in a terminal
		
		/*
		ModelAI ai_smart = new ModelAI(
			"weights/ai/ai_draw_5deep.txt",
			"weights/ai/ai_play_5deep.txt",
			rack_size, true, false
		);
		ModelAI ai_dumb = new ModelAI(
			ai_smart, true, true
		);
		*/
		//Model baltar = null, casandra = null, ensemble = null;
		//Model diablo1 = new ModelDiablo("weights/diablo/diablo_weights10_2_0frozen.txt", false);
		ModelDiablo diablo2 = new ModelDiablo("weights/diablo/diablo2_weights.txt", false);
		/*
		try{
			baltar = new ModelBaltar();
			casandra = new ModelCasandra(
				"weights/casandra/c_draw_diablo.txt",
				"weights/casandra/c_play_diablo.txt",
				diablo1, 5
			);
			ensemble = new ModelEnsemble();
		} catch (Exception e){
			System.out.println("Could not load Baltar files!!!!");
		}
		*/
		
		Player[] players = new Player[]{
			//new PlayerComputer(ensemble),
			//new PlayerComputer(new ModelKyle(false)),
			new PlayerComputer(diablo2)
			//new PlayerComputer(new ModelDiablo(diablo2, true))
			//new PlayerComputer(new ModelKyle(true)),
			//new PlayerComputer(new ModelMax())
		};

		//TRAINING & TESTING
		if (play_human){
			System.out.println("\n--------  RACKO  --------");
			System.out.println("Cards go from 1 to "+(rack_size*3+rack_size*players.length+"\n"));
			Game.verbose = true;
			if (args.length != 0 && args[0].equals("spymode"))
				Game.spymode = true;
			players = Arrays.copyOf(players, players.length+1);
			players[players.length-1] = new PlayerConsole();
		}
			
		Game g = Game.create(players, rack_size, streak_min, bonus_mode);
		g.limitMoves(move_limit);
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
						round(p.EPOCH_allmoves)+"\t\t"+
						round(p.EPOCH_badmoves*100)+"%\t\t"+
						round(p.EPOCH_wins*100)+"%\t\t"+
						round(p.MODEL_allmoves)+"\t\t"+
						round(p.MODEL_badmoves*100)+"%\t\t"+
						round(p.MODEL_wins*100)+"%"
					);
				}
			}
		}
		//*/
	}
	
	private static void testSuite(){
		/* TESTS
		x	[5,7,4,3,6,9]
		x	[1,7,5,6,9]
		x	[3,7,5,2,6]
		x	[1,7,5,4,6,9]
		x	[1,2,3,4,5]
		x	[5,4,3,2,1]
		x	[2,3,1,7,9,5,8]
		x	[2,3,1,7,5]
		x	[1,9,7,10]
		x	[4,1,3,2]
		x	[7,1,6,3,2]
		x	[7,9,6,10,5,8]
		*/
		int[] hand = new int[]{1,26,27,28,29,30,31,2};
		Player[] players = new Player[]{
			new PlayerConsole(),
			new PlayerConsole()
		};
		Game g = Game.create(players, hand.length, 1, false);
		Rack r = players[0].rack;
		r.deal(hand);
		
		
		//Score metrics testing
		System.out.println("MaxCard = "+g.card_count);
		System.out.println("Points = "+r.scorePoints(false));
		System.out.println("RackDE = "+r.scoreRackDE(g.dist_flat, null));
		System.out.println("RackDE' = "+r.scoreRackDE(g.dist_flat, g.dist_skew));
		ArrayList<Rack.LUS> lus = r.getLUS(false);
		for (Rack.LUS l: lus){
			System.out.println(Arrays.toString(l.cards));
			System.out.println("\tDensityCenter = "+r.scoreDensityCenter(l, null));
			//*
			System.out.println("\tClumpDE = "+r.scoreClumpDE(l, g.dist_flat, null));
			System.out.println("\tClumpDE' = "+r.scoreClumpDE(l, g.dist_flat, g.dist_skew));
			System.out.println("\tDensity0 = "+r.scoreDensityAdjacent(l, null, 0));
			System.out.println("\tDensity0' = "+r.scoreDensityAdjacent(l, g.dist_skew, 0));
			System.out.println("\tDensity1 = "+r.scoreDensityAdjacent(l, null, 1));
			System.out.println("\tDensity1' = "+r.scoreDensityAdjacent(l, g.dist_skew, 1));
			//*
			System.out.println("\tProbReal = "+r.scoreProbability(l, null, false, true, 0));
			System.out.println("\tProbReal' = "+r.scoreProbability(l, g.dist_skew, false, true, 0));
			System.out.println("\tProbAvg = "+r.scoreProbability(l, null, true, true, 0));
			System.out.println("\tProbAvg' = "+r.scoreProbability(l, g.dist_skew, true, true, 0));
			//*/
		}
	}
	private static double round(double val){
		return Math.round(val*100)/100.0;
	}
}
