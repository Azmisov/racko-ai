package interfaces;

/**
 * A distribution function; basically, any 2D algebraic function
 * X-Axis = position in rack (zero indexed)
 * Y-Axis = card number (one indexed)
 */
public abstract class Distribution {
	/**
	 * Return the distribution value for this input
	 * @param x the x value 
	 * @return the "y" value
	 */
	public abstract double eval(int x);
}
