package racko;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controls a Racko game with fixed settings
 */
public class Game {
	//Scoring constants
	public static int
		score_win = 500;	//score needed to win the game
	
	//Game management
	public final int rack_size, min_streak, player_count;
	public final boolean bonus_mode;
	public final Deck deck;
	private final Player[] players;
	private final Rack[] racks;
	private int current_player;
	
	//Game statistics
	private final int[] stat_scores, stat_wins;
	
	/**
	 * Creates a new racko game
	 * @param playerList a list of players
	 * @param rackSize how large each rack should be
	 * @param minStreak minimum streak to win the game (use 1 for standard rules)
	 * @param bonusMode allow bonus points for streaks
	 */
	private Game(Player[] playerList, int rackSize, int minStreak, boolean bonusMode){
		player_count = playerList.length;
		this.players = playerList;
		racks = new Rack[player_count];
		rack_size = rackSize;
		min_streak = minStreak;
		bonus_mode = bonusMode;
		
		stat_scores = new int[player_count];
		stat_wins = new int[player_count];
		
		//Create the deck; rack size and player count are validated here
		deck = new Deck(rackSize, player_count);
		deck.addDiscardListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				turn();
			}
		});
	}
	/**
	 * Registers players to the game
	 */
	private void register(){
		for (int i=0; i<player_count; i++){
			Rack r = new Rack(rack_size);
			players[i].register(this, r);
			racks[i] = r;
		}
	}
	/**
	 * Creates a new racko game and register players
	 * @param players a list of players
	 * @param rackSize how large each rack should be
	 * @param minStreak minimum streak to win the game (use 1 for standard rules)
	 * @param bonusMode allow bonus points for streaks
	 */
	private static void create(Player[] players, int rackSize, int minStreak, boolean bonusMode){
		Game g = new Game(players, rackSize, minStreak, bonusMode);
		g.register();
	}
	
	/**
	 * Starts a new game
	 */
	public void play(){
		deck.deal(racks);
		current_player = 0;
		players[0].play();
	}
	/**
	 * Compute game logic after each player's turn
	 */
	private void turn(){
		boolean new_round = false, new_turn = true;
		//Check if this player has won
		Rack cur_rack = racks[current_player];
		if (cur_rack.isSorted()){
			if (min_streak < 2 || cur_rack.maxStreak() >= min_streak){
				new_turn = false;
				//We have a winner
				stat_wins[current_player]++;
				int max_score = 0, max_idx = 0;
				for (int i=0; i<player_count; i++){
					int score = racks[i].score(bonus_mode);
					stat_scores[i] += score;
					players[i].scoreRound(i == current_player, score);
					//Check if they have greater than "score_win" points
					//In case of a tie, go for the person that won the most games
					//TODO: what to do when it is still a tie???
					if (stat_scores[i] >= score_win &&
						(stat_scores[i] > max_score || stat_scores[i] == max_score && stat_wins[i] > stat_wins[max_idx]))
					{
						max_score = stat_scores[i];
						max_idx = i;
					}
				}
				if (max_score == 0)
					new_round = true;
				//Notify players that the game has ended
				else{
					for (int i=0; i<player_count; i++)
						players[i].scoreGame(i == max_idx, stat_scores[i]);
				}
			}
		}
		//Get next player to play
		if (new_turn){
			current_player = (current_player+1) % player_count;
			players[current_player].play();
		}
		//Start a new round
		else if (new_round)
			play();
	}
}
