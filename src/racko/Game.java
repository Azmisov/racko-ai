package racko;

import client.PlayerConsole;
import distributions.DistributionFlat;
import distributions.DistributionSkew;
import interfaces.GUI;
import interfaces.Player;

/**
 * Controls a Racko game with fixed settings
 * TODO: be able to play multiple games at once?
 */
public class Game {
	//Shows rack output for each move
	public static boolean verbose = false, spymode = false;
	
	//Scoring constants
	public static int
		score_win = 500,	//score needed to win the game
		score_all = 25,			//score for having all cards in order
		score_single = 5,		//score for a single card in order
		score_bonus = 50,		//bonus score for the minimum streak
		score_bonus_fac = 2,	//bonus score multiplication factor for each additional card above minimum streak
		bonus_min = 3,			//minimum streak for bonus
		bonus_max = 6;			//maximum streak for bonus
	public DistributionSkew dist_skew;
	public DistributionFlat dist_flat;
	
	//Game management
	private GUI gui = null;
	public final int rack_size, min_streak, player_count, card_count;
	public final boolean bonus_mode;
	public final Deck deck;
	private final Player[] players;
	private int active_player, move_limit;
	
	/**
	 * Creates a new racko game
	 * @param playerList a list of players
	 * @param rackSize how large each rack should be
	 * @param minStreak minimum streak to win the game (use 1 for standard rules)
	 * @param bonusMode allow bonus points for streaks
	 */
	private Game(Player[] playerList, int rackSize, int minStreak, boolean bonusMode){
		player_count = playerList.length;
		players = playerList;
		rack_size = rackSize;
		min_streak = minStreak;
		bonus_mode = bonusMode;
		
		//Create the deck; rack size and player count are validated here
		deck = new Deck(players, rackSize);
		card_count = deck.cards;
		
		//Create distribution objects
		dist_skew = new DistributionSkew(rack_size, card_count, 1);
		dist_flat = new DistributionFlat(rack_size, card_count);
	}
	/**
	 * Registers players to the game
	 */
	private void register(){
		int max = deck.cards;
		for (int i=0; i<player_count; i++){
			Rack r = new Rack(rack_size, this);
			players[i].register(this, r);
		}
	}
	/**
	 * Creates a new racko game and register players
	 * @param players a list of players
	 * @param rackSize how large each rack should be
	 * @param minStreak minimum streak to win the game (use 1 for standard rules)
	 * @param bonusMode allow bonus points for streaks
	 */
	public static Game create(Player[] players, int rackSize, int minStreak, boolean bonusMode){
		Game g = new Game(players, rackSize, minStreak, bonusMode);
		g.register();
		return g;
	}
	/**
	 * Register a gui for callbacks
	 * @param gui 
	 */
	public void registerGUI(GUI gui){
		this.gui = gui;
		deck.registerGUI(gui);
	}
	
	/**
	 * Limits the number of moves in a game before calling a draw
	 * @param limit move limit; use 0 for unlimited
	 */
	public void limitMoves(int limit){
		move_limit = limit;
	}
	
	/**
	 * Starts a new game
	 * @param start_player who should start the game?
	 */
	public void play(int start_player){
		for (Player player: players){
			player.score = 0;
			player.wins = 0;
			player.beginGame();
		}
		if (gui != null)
			gui.beginGame();
		
		//Outer loop sets up games for each new round
		while (true){
			//Deal out a new deck; setup variables for the game loop
			deck.deal();
			active_player = start_player-1;
			if (gui != null)
				gui.beginRound();
			
			//Inner loop goes through each player, starting with 0
			while (true){
				//Get next player to play
				active_player = (active_player+1) % player_count;
				Player cur_player = players[active_player];
				Rack cur_rack = cur_player.rack;
				if (gui != null)
					gui.turn(cur_player, active_player, cur_rack);
				deck.discard(cur_player.play());
				cur_player.STAT_allmoves++;

				//Check if this player has won
				boolean won = cur_rack.isSorted();
				
				//Show output for human player
				if (Game.verbose && !(cur_player instanceof PlayerConsole)){
					String spy = won || spymode ? cur_rack.toString() : cur_rack.toStringVisible();
					System.out.println("COMPUTER-P"+cur_player.playerNumber+": "+spy);
				}

				//If they haven't won, check for a draw
				int lowest_score = -1, lowest_score_player = 0;
				boolean draw = move_limit > 0 && cur_player.STAT_allmoves >= move_limit && !won;
				if (won || draw){
					if (draw || min_streak < 2 || cur_rack.maxStreak() >= min_streak){
						//We have a winner
						if (won){
							cur_player.STAT_wins++;
							cur_player.wins++;
						}
						int max_score = 0, max_idx = 0;
						for (int i=0; i<player_count; i++){
							Player p = players[i];
							int score = p.rack.scorePoints(bonus_mode);
							if (lowest_score == -1 || score < lowest_score){
								lowest_score = score;
								lowest_score_player = i;
							}
							p.score += score;
							p.STAT_score += score;
							p.STAT_rounds++;
							p.scoreRound(draw ? false : i == active_player, score);
							//Check if they have greater than "score_win" points
							//In case of a tie, go for the person that won the most games
							//TODO: what to do when it is still a tie???
							if (p.score >= score_win &&
								(p.score > max_score || p.score == max_score && p.wins > players[max_idx].wins))
							{
								max_score = p.score;
								max_idx = i;
							}
						}
						if (gui != null)
							gui.scoreRound(won ? cur_player : null, won ? active_player : 0);
						//No one has reached "score_win" points; play next round
						if (max_score == 0){
							start_player = lowest_score_player;
							break;
						}
						//Notify players that the game has ended
						else{
							for (int i=0; i<player_count; i++)
								players[i].scoreGame(i == max_idx, players[i].score);
							if (gui != null)
								gui.scoreGame(players[max_idx], max_idx);
							return;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Calculates the maximum number of points a player
	 * could earn in a round
	 * @return maximum points possible per round
	 */
	public int maxPoints(){
		//Create a dummy rack and use scorePoints to get max score
		Rack dummy = new Rack(rack_size, this);
		int[] best = new int[rack_size];
		for (int i=0; i<rack_size; i++)
			best[i] = i+1;
		dummy.deal(best);
		return dummy.scorePoints(bonus_mode);
	}
}