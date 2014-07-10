package client;

import interfaces.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import javax.swing.*;
import models.*;
import racko.*;

/**
 * Human usable interface for playing racko
 */
public class Main extends JFrame implements GUI{
	//Window title
	private static final String title = "Rack-O v0.2";
	private static int WIN_WIDTH = 750, WIN_HEIGHT, WIN_DEC_HEIGHT = 26;
	//GUI items
	private CardLayout views;
	private Board board;
	private boolean spymode_always = false, end_of_game = false;
	//Game variables
	private static int rack_size;
	private final Game game;
	private GameThread gthread;
	private final Player player_ai, player_human;
	//Player GUI requests
	private PlayerGUI requester;
	private int request_type = 0;	//0=empty, 1=discard, 2=slot
	private int current_player;
	
	private Main(int rack_size, Player ai) throws Exception{
		Main.rack_size = rack_size;
		WIN_HEIGHT = rack_size*Card.height+(rack_size-1)*Board.card_pad+2*Board.border_pad_ver+50;
		if (WIN_HEIGHT < 300) WIN_HEIGHT = 400;
		
		player_ai = ai;
		player_human = new PlayerGUI(this);
		game = Game.create(
			new Player[]{player_human, player_ai},
			rack_size, 1, false
		);
		Card.card_count = game.card_count;
		game.registerGUI(this);
		addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyChar() == 's')
					spymode_always = board.toggleSpyMode();
			}
		});
		System.out.println("Use s-key to toggle spy mode");
		
		initGUI();
	}
	public static void main(final String[] args){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run(){
				Main win;
				try {
					int rsize = 0;
					Player ai = null;
					try{
						rsize = Integer.parseInt(args[0]);
						if (rsize <= 2) throw new Exception("Invalid rack size");
						Model m;
						switch(args[1]){
							case "diablo":
								m = new ModelDiablo("/home/isaac/Programming/racko-ai/weights/diablo/diablo2_weights10_2_wkyletraining.txt", false);
								break;
							case "baltar":
								m = new ModelBaltar();
								break;
							case "kyle":
								m = new ModelKyle(false);
								break;
							case "max":
								m = new ModelMax();
								break;
							case "random":
								m = new ModelRandom();
								break;
							default:
								throw new Exception("Invalid AI name");
						}
						ai = new PlayerComputer(m);
					} catch(Exception e){
						rsize = 0;
						System.out.println("Error: "+e);
						System.out.println("Usage: [rack_size] [diablo|baltar|kyle|max|random]");
					}
					if (rsize != 0){
						win = new Main(rsize, ai);
						win.setVisible(true);
						win.startGameThread();
					}
				} catch (Exception ex) {
					System.out.println("Exception thrown in Main:");
					System.out.println(ex.getMessage());
				}
			}
		});
	}
	
	//GUI INITIALIZATION
	private void initGUI(){
		//TODO
		//Switches between Main views (e.g. game mode / settings mode)
		//views = new CardLayout();
		setTitle(title);
		setSize(WIN_WIDTH, WIN_HEIGHT+WIN_DEC_HEIGHT);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		setResizable(false);
		
		board = new Board(player_ai.name);
		add(board, BorderLayout.CENTER);
		
		board.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				hitTest(e.getX(), e.getY());
			}
		});
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

	//GAME THREAD FEEDBACK
	@Override
	public void turn(Player p, int player_index, Rack r){
		current_player = player_index;
		board.turn(player_index);
		//Delay, if it is the computer's turn
		if (player_index == 1){
			try {
				Thread.sleep(500);
			} catch (InterruptedException ex) {}
		}
	}
	@Override
	public void draw(int card, boolean fromDiscard){
		board.playDraw(card, fromDiscard, game.deck.peek(true), current_player == 0);
		try {
			Thread.sleep(500);
		} catch (InterruptedException ex) {}
	}
	@Override
	public void discard(int card){
		//Check if we're discarding the drawn card
		if (card == board.animate.card)
			board.playDiscard(board.animate);
		else{
			Card[] arr = current_player == 0 ? board.rack_human : board.rack_ai;
			for (int i=0; i<rack_size; i++){
				if (card == arr[i].card){
					board.playDiscard(arr[i]);
					break;
				}
			}
		}
	}
	@Override
	public void scoreRound(Player winner, int player_index){
		end_of_game = true;
		board.showEndGame(player_index == 0, true);
		pauseGameThread(true);
	}
	@Override
	public void scoreGame(Player winner, int player_index){}
	@Override
	public void beginRound(){
		board.spymode = spymode_always;
		board.loadRacks(player_ai, player_human);
		board.setDiscard(game.deck.peek(true));
	}
	@Override
	public void beginGame(){}
	@Override
	public void requestDiscard(PlayerGUI p){
		requester = p;
		request_type = 1;
		pauseGameThread(true);
	}
	@Override
	public void requestSlot(PlayerGUI p){
		requester = p;
		request_type = 2;
		pauseGameThread(true);
	}
	
	//MOUSE LISTENER
	private void hitTest(int x, int y){
		//Check for if they click the "play again" button
		if (end_of_game){
			if (board.new_game.hitTest(x, y)){
				end_of_game = false;
				board.showEndGame(false, false);
				pauseGameThread(false);
			}
		}
		//Hit tests during play
		else if (request_type > 0){
			Card hit = board.hitTest(x, y);
			if (hit != null){
				//Notify player-gui of the draw choice
				boolean is_discard = hit.equals(board.pile_discard),
						is_draw = !is_discard && hit.equals(board.pile_draw);
				if (request_type == 1){
					//invalid draw click point
					if (!is_discard && !is_draw)
						return;
					requester.guiDiscard = is_discard;
				}
				//invalid play click point
				else if (is_draw) return;
				else{
					if (is_discard)
						requester.guiSlot = -1;
					else{
						int slot = 0;
						for (; slot<rack_size; slot++){
							if (board.rack_human[slot].equals(hit))
								break;
						}
						requester.guiSlot = slot;
					}
				}
				request_type = 0;
				pauseGameThread(false);
			}
		}
	}
	
	//DRAWING CODE
	private static class Board extends JPanel{
		private static final Font title_font = new Font("Sans Serif", Font.BOLD, 25);
		private static final int
			border_pad_hor = 50, border_pad_ver = 10, pile_pad = 20, card_pad = 5,
			right_offset = 4, score_width = 32;
		private static final long animate_timestep = 50;
		private static final HashMap<RenderingHints.Key, Object> antialias =
			new HashMap<RenderingHints.Key, Object>(){{
				put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			}};
		public boolean loaded = false, spymode = false, animate_visible = false, won = false;
		public final Card[] rack_ai;
		public final Card[] rack_human;
		public Card pile_discard, pile_draw, animate;
		public final int pile_start_x, pile_start_y, rack_start_y;
		public Button new_game;
		private int current_player;
		private String ai_name;
		
		public Board(String ai_name){
			this.ai_name = ai_name;
			rack_ai = new Card[rack_size];
			rack_human = new Card[rack_size];
			
			//Setup gui items
			pile_start_x = WIN_WIDTH/2 - Card.width/2;
			pile_start_y = WIN_HEIGHT/2 - Card.height/2;
			rack_start_y = WIN_HEIGHT/2 - (Card.height*rack_size + card_pad*(rack_size-1))/2;
			pile_draw = new Card(0, pile_start_x, pile_start_y, false);
			pile_discard = new Card(0, pile_start_x, pile_start_y+Card.height+pile_pad, true);
			animate = new Card(0, 0, 0, true);
			new_game = new Button("Play Again", WIN_WIDTH/2, 60);
		}
		
		public void loadRacks(Player ai, Player human){
			loaded = ai.rack != null && human != null;
			if (!loaded) return;
			
			for (int i=0; i<rack_size; i++){
				rack_human[i] = new Card(
					human.rack.getCardAt(i),
					border_pad_hor,
					i*(Card.height+card_pad) + rack_start_y,
					true
				);
				rack_ai[i] = new Card(
					ai.rack.getCardAt(i),
					WIN_WIDTH-border_pad_hor-Card.width-right_offset,
					i*(Card.height+card_pad) + rack_start_y,
					ai.rack.isVisible(i)
				);
			}
			repaint();
		}
		public Card hitTest(int x, int y){
			for (int i=0; i<rack_size; i++){
				if (rack_human[i].hitTest(x, y))
					return rack_human[i];
			}
			if (pile_discard.hitTest(x, y))
				return pile_discard;
			if (pile_draw.hitTest(x, y))
				return pile_draw;
			return null;
		}
		
		public void turn(int player_idx){
			current_player = player_idx;
			repaint();
		}
		public void playDiscard(Card s){
			//If we're discarding the drawn card, skip the swap animation
			if (!s.equals(animate)){
				//Swap animation
				animate(s.x, s.y, 450);
				int temp = s.card;
				s.card = animate.card;
				animate.card = temp;
			}
			animate.visible = true;
			animate(pile_discard.x, pile_discard.y, 450);
			pile_discard.card = animate.card;
			animate_visible = false;
			repaint();
		}
		public void playDraw(int card, boolean fromDiscard, int new_discard, boolean visible){
			animate_visible = true;
			animate.visible = visible || fromDiscard;
			animate.x = fromDiscard ? pile_discard.x : pile_draw.x;
			animate.y = fromDiscard ? pile_discard.y : pile_draw.y;
			animate.card = card;
			if (fromDiscard)
				pile_discard.card = new_discard;
			animate(animate.x, pile_start_y-Card.height-pile_pad, 300);
		}
		public void setDiscard(int card){
			pile_discard.card = card;
		}
		private void animate(int target_x, int target_y, int duration){
			float time = duration/(float) animate_timestep;
			int steps = (int) time;
			if (steps < time) steps++;
			float dx = (target_x - animate.x)/time,
				  dy = (target_y - animate.y)/time;
			
			try {
				for (int i=0; i<steps; i++){
					animate.x += dx;
					animate.y += dy;
					repaint();
					Thread.sleep(animate_timestep);
				}
			} catch (Exception e){
				System.out.println("Animation failed!!!");
			}
		}

		public void showEndGame(boolean won, boolean show){
			this.won = won;
			new_game.visible = show;
			spymode = true;
			repaint();
		}
		public boolean toggleSpyMode(){
			spymode = !spymode;
			repaint();
			return spymode;
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (!loaded) return;
			
			//Enable antialiasing
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHints(antialias);
			
			//Help & other text
			g2.setColor(Color.darkGray);
			g2.setFont(title_font);
			FontMetrics fm = g2.getFontMetrics(Card.cardfont);
			if (new_game.visible){
				String str = won ? "You won the round!" : "You lost the round";
				Rectangle2D bounds = fm.getStringBounds(str, g);
				g2.drawString(str,
					(int) (WIN_WIDTH/2-bounds.getWidth()/2.65),
					(int) (bounds.getHeight()+10)
				);
				new_game.y = (int) (bounds.getHeight()+40);
				new_game.paint(g2);
			}
			else{
				String str = current_player == 0 ? "Human" : ai_name;
				Rectangle2D bounds = fm.getStringBounds(str, g);
				int x = (int) (WIN_WIDTH/2-bounds.getWidth()/2.65),
					y = (int) (bounds.getHeight()+10);
				g2.drawString(str, x, y);
				int x_start = x-15 + (int) (current_player == 0 ? 0 : bounds.getWidth()),
					x_offset = current_player == 0 ? -17 : 17,
					y_shift = 3,
					y_start = (int) (y-bounds.getHeight()/2)+y_shift,
					y_offset = (y-y_start)/2+y_shift;
				g2.fillPolygon(
					new int[]{x_start, x_start, x_start+x_offset},
					new int[]{y_start, y_start+y_offset*2, y_start+y_offset},
					3
				);
			}
			
			//Decks
			pile_draw.paint(g2, false);
			if (pile_discard.card != 0)
				pile_discard.paint(g2, spymode);
			if (animate_visible)
				animate.paint(g2, spymode);
			
			//Scoring guides
			g2.setColor(Color.darkGray);
			g2.fillRect(0, 0, score_width, WIN_HEIGHT);
			g2.fillRect(WIN_WIDTH-score_width-right_offset, 0, score_width, WIN_HEIGHT);
			g2.setColor(Color.lightGray);
			g2.setFont(Card.rackofont);
			for (int i=0; i<rack_size; i++){
				int y = i*(Card.height+card_pad) + rack_start_y + (Card.height/2+6);
				String score = Integer.toString((i+1)*5);
				g2.drawString(score, 5, y);
				g2.drawString(score, WIN_WIDTH-Board.right_offset-8*(score.length())-10, y);
			}
			
			//Slots
			for (int i=0; i<rack_size; i++){
				rack_ai[i].paint(g2, spymode);
				rack_human[i].paint(g2, spymode);
			}
		}
	}
	private static class Card{
		private static final int round = 8, width = 120, height = 60, fontsize = 40;
		private static final Font
			cardfont = new Font("Sans Serif", Font.PLAIN, Card.fontsize),
			rackofont = new Font("Serif", Font.PLAIN, 15);
		private static final float hue_lo = .2f, hue_hi = .8f;
		private static int card_count;
		private int x = 20, y = 20, card = 0;
		private boolean visible = false;
		
		public Card(int card, int x, int y, boolean visible){
			this.card = card;
			this.x = x;
			this.y = y;
			this.visible = visible;
		}	
		public void paint(Graphics2D g, boolean spymode){			
			if (visible || spymode){
				//Draw slot number
				float hue = card*(hue_hi-hue_lo) + card_count*hue_lo - hue_hi;
				Color card_col = Color.getHSBColor(hue / (float) (card_count-1), .3f, 1f);
				g.setColor(card_col);
				g.fillRoundRect(x, y, width, height, round, round);
				
				g.setColor(Color.darkGray);
				String num = Integer.toString(card);
				g.setFont(Card.cardfont);
				g.drawString(num, x+width/2-12*num.length(), y+(fontsize+height-10)/2);
			}
			else{
				g.setColor(Color.darkGray);
				g.fillRoundRect(x, y, width, height, round, round);
			
				g.setColor(Color.lightGray);
				g.setFont(Card.rackofont);
				g.drawString("Rack-O", x+(int) (width/2.35), y+(int) (height/1.3));
			}
			
			g.setColor(Color.black);
			g.drawRoundRect(x, y, width, height, round, round);
		}
		public boolean hitTest(int mx, int my){
			return mx >= x && mx <= x+width && my >= y && my <= y+height;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Card))
				return false;
			Card cobj = (Card) obj;
			return card == cobj.card && x == cobj.x && y == cobj.y;
		}
	}
	private static class Button{
		private static final Font font = new Font("Sans Serif", Font.BOLD, 25);
		private static final int padding_hor = 12, padding_ver = 8, round = 5;
		private final String text;
		private int x, y, box_x, box_w, box_h;
		private double w, h;
		public boolean visible = false, has_rendered = false;
		
		public Button(String text, int x, int y){
			this.text = text;
			this.x = x;
			this.y = y;
		}
		
		public void paint(Graphics2D g){
			g.setFont(font);
			if (!has_rendered){
				FontMetrics metrics = g.getFontMetrics(font);
			
				Rectangle2D bounds = metrics.getStringBounds(text, g);
				w = bounds.getWidth();
				h = bounds.getHeight();
				box_x = (int) (x-w/2-padding_hor);
				box_w = (int) (w+padding_hor*2);
				box_h = (int) (h+padding_ver*2);
				has_rendered = true;
			}
			
			g.setColor(Color.darkGray);
			g.fillRoundRect(box_x, y, box_w, box_h, round, round);
			g.setColor(Color.black);
			g.drawRoundRect(box_x, y, box_w, box_h, round, round);
			g.setColor(Color.getHSBColor(0, 0, .65f));
			g.drawString(text, (int) (x-w/2), (int) (y+padding_ver+h-6));
		}
		public boolean hitTest(int mx, int my){
			if (!has_rendered)
				return false;
			return mx >= box_x && mx <= box_x+box_w && my >= y && my <= y+box_h;
		}
	}
	
	//GAME THREAD
	private void startGameThread(){
		gthread = new GameThread(game);
		gthread.start();
	}
	private void pauseGameThread(boolean pause){
		synchronized(gthread){
			try {
				if (pause) gthread.wait();
				else gthread.notify();
			} catch (InterruptedException ex) {
				System.out.println("Failed to pause/start gthread!!!");
			}
		}
	}
	private class GameThread extends Thread{
		private Game game;
		public GameThread(Game game){
			this.game = game;
		}
		@Override
		public void run() {
			//GUI doesn't do anything with scores/games
			//so, we'll just run this in an infinite loop
			while (true)
				game.play(0);
		}
	}
}
