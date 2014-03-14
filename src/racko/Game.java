package racko;

import interfaces.Player;

/**
 * Controls a Racko game with fixed settings
 */
public class Game {
	//Scoring constants
	public static int
		score_win = 500;	//score needed to win the game
	
	//Game management
	public final int rack_size, min_streak, player_count, card_count;
	public final boolean bonus_mode;
	public final Deck deck;
	private final Player[] players;
	private int current_player;
	
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
	}
	/**
	 * Registers players to the game
	 */
	private void register(){
		int max = deck.getMaxCard();
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
	public void play(){
		for (int i=0; i<players.length; i++){
			players[i].movesInGame = 0;
			players[i].wins = 0;
			players[i].score = 0;
			players[i].beginGame();
		}
		
		//Outer loop sets up games for each new round
		while (true){
			//Deal out a new deck; setup variables for the game loop
			deck.deal();
			current_player = -1;
			
			//Inner loop goes through each player, starting with 0
			while (true){
				//Get next player to play
				current_player = (current_player+1) % player_count;
				players[current_player].movesInRound++;
				players[current_player].movesInGame++;
				deck.discard(players[current_player].play());

				//Check if this player has won
				Rack cur_rack = players[current_player].rack;
				if (cur_rack.isSorted()){
					if (min_streak < 2 || cur_rack.maxStreak() >= min_streak){
						//We have a winner
						players[current_player].wins++;
						int max_score = 0, max_idx = 0;
						for (int i=0; i<player_count; i++){
							Player p = players[i];
							int score = p.rack.scorePoints(bonus_mode);
							p.score += score;
							p.scoreRound(i == current_player, score);
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
}