package distributions;

import interfaces.Distribution;

/**
 * Skewed distribution for error weighting
 * This is a linear skew; I see no reason behind doing a nonlinear skew
 * Positive skew = (0, skew), Negative skew = (rack_size, -skew)
 * @author isaac
 */
public class DistributionSkew extends Distribution{
	private double slope, offset;
	
	/**
	 * Create a skewed distribution
	 * @param rack_size
	 * @param max_card
	 * @param skew between -1 and 1, otherwise it will be clamped
	 */
	public DistributionSkew(int rack_size, int max_card, double skew){
		super(rack_size, max_card);
		//Clamp skew value, so we can keep the error weights normalized between 0-1
		if (skew > 1) skew = 1;
		else if (skew < -1) skew = -1;
		//Compute skew line
		offset = skew > 0 ? skew : 0;
		slope = -skew / (double) rack_size;
	}

	@Override
	public double eval(double x) {
		return slope*x+offset;
	}
}
