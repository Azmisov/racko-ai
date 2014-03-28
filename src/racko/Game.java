package racko;

import client.PlayerHuman;
import distributions.DistributionFlat;
import distributions.DistributionSkew;
import interfaces.Player;

/**
 * Controls a Racko game with fixed settings
 */
public class Game {
	//Shows rack output for each move
	public static boolean verbose = false;
	
	//Scoring constants
	public static int
		score_win = 500;	//score needed to win the game
	public DistributionSkew dist_skew;
	public DistributionFlat dist_flat;
	
	//Game management
	public final int rack_size, min_streak, player_count, card_count;
	public final boolean bonus_mode;
	public final Deck deck;
	private final Player[] players;
	private int active_player;
	
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
			Rack r = new Rack(rack_size, max);
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
	 * Starts a new game
	 */
	public void play(int start_player){
		for (int i=0; i<players.length; i++)
			players[i].beginGame();
		
		//Outer loop sets up games for each new round
		while (true){
			//Deal out a new deck; setup variables for the game loop
			deck.deal();
			active_player = start_player-1;
			
			//Inner loop goes through each player, starting with 0
			while (true){
				//Get next player to play
				active_player = (active_player+1) % player_count;
				Player cur_player = players[active_player];
				deck.discard(cur_player.play());
				cur_player.STAT_allmoves++;
				
				//Show output for human player
				Rack cur_rack = cur_player.rack;
				if (Game.verbose && !(cur_player instanceof PlayerHuman))
					System.out.println("\tP"+cur_player.playerNumber+": "+cur_rack.toString());

				//Check if this player has won
				if (cur_rack.isSorted()){
					if (min_streak < 2 || cur_rack.maxStreak() >= min_streak){
						//We have a winner
						cur_player.STAT_wins++;
						cur_player.wins++;
						int max_score = 0, max_idx = 0;
						for (int i=0; i<player_count; i++){
							Player p = players[i];
							int score = p.rack.scorePoints(bonus_mode);
							p.score += score;
							p.STAT_score += score;
							p.STAT_rounds++;
							p.scoreRound(i == active_player, score);
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
						//No one has reached "score_win" points; play next round
						if (max_score == 0) break;
						//Notify players that the game has ended
						else{
							for (int i=0; i<player_count; i++)
								players[i].scoreGame(i == max_idx);
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
		Rack dummy = new Rack(rack_size, card_count);
		int[] best = new int[rack_size];
		for (int i=0; i<rack_size; i++)
			best[i] = i+1;
		dummy.deal(best);
		return dummy.scorePoints(bonus_mode);
	}
}