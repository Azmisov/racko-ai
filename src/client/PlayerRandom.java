/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import interfaces.Player;
import java.util.Random;

/**
 *
 * @author isaac
 */
public class PlayerRandom extends Player{
	private static Random rand = new Random();
	
	@Override
	public int play() {
		STAT_badmoves++;
		boolean mary_poppins = rand.nextBoolean();
		int card = game.deck.draw(mary_poppins);
		int pos = rand.nextInt(game.rack_size+1);
		if (pos == game.rack_size)
			return card;
		return rack.swap(card, pos, mary_poppins);
	}
	
}
