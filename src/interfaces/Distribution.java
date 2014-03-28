package interfaces;

/**
 * A distribution function; basically, any 2D algebraic function
 * X-Axis = position in rack (zero indexed)
 * Y-Axis = card number (one indexed); or error weight, between 0-1 (for SSE)
 */
public abstract class Distribution {
	protected int rack_size, max_card;
			
	/**
	 * Create a distribution
	 * @param rack_size
	 * @param max_card 
	 */
	public Distribution(int rack_size, int max_card){
		this.rack_size = rack_size;
		this.max_card = max_card;
	}
	
	/**
	 * Return the distribution value for this input
	 * @param x the x value (position in rack)
	 * @return the "y" value (card number or error weight)
	 */
	public abstract double eval(double x);
}
