/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package client;

import interfaces.Player;

/**
 *
 * @author isaac
 */
public class PlayerDiablo extends Player{

	@Override
	public int play() {
		/* FEATURES TO USE
			- bonusMode:
				may optimize for denser clumps to get bonus points
			- min_streak/rack_size:
				optimize for dense clumps to actually win the game
			- turns/rack_size:
				if closer to one, the game is close to ending,
				so may want to optimize to get more points if not close to winning
			- sequence_length/rack_size:
				maximize usable sequence length
			- scorePoints/max_points:
				maximize points scored
			- scoreRackDE(flat, null):
				get a rack with flat, spread out distribution
			- scoreClumpDE(seq, flat, null):
				same as previous, except ignoring unusable cards
			- scoreProbability(seq, null, false, true, 0):
				maximize probability of getting a larger sequence
			- scoreProbability(seq, null, true, true, 0):
				same as previous, except does not penalize as much for low probabilities
			- scoreDensity(seq, null, 0):
				maximize density of clumps in a sequence
			- scoreDensity(seq, null, 1):
				same as previous, except penalizes for clumps of only length 1

			SKEWED FEATURES
			All the same as before, except optimizes for a higher score if the player doesn't win
			- scoreRackDE(flat, skew)
			- scoreClumpDE(seq, flat, skew)
			- scoreProbability(seq, skew, false, true, 0)
			- scoreProbability(seq, skew, true, true, 0)
			- scoreDensity(seq, skew, 0):
			- scoreDensity(seq, skew, 1):

			Other features to consider:
			- scoreDensity weighted by probabilities or distribution-error
			- scoreClumpDE weighted by probabilities or density
			- scoreProbability weighted by distribution-error
		
			Tree traversal techniques:
			- just take output of getLSU()
			- use probabilities/density/clumpDE as a UsableMetric for getLSU()
			- use probabilties/... to generate new sequences from getLSU() output
		*/
		return 0;
	}
}
