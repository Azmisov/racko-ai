package client;

import interfaces.GUI;
import interfaces.Player;

/**
 * Adds hooks for a GUI to control a player's actions
 * @author isaac
 */
public class PlayerGUI extends Player{
	private final GUI gui;
	public boolean guiDiscard;
	public int guiSlot;
	
	public PlayerGUI(GUI gui){
		super();
		this.gui = gui;
	}
	
	@Override
	public int play(){
		gui.requestDiscard(this);
		int card = game.deck.draw(guiDiscard);
		gui.requestSlot(this);
		return guiSlot == -1 ? card : rack.swap(card, guiSlot, guiDiscard);
	}
}
