package distributions;

import interfaces.Distribution;

/**
 * Flat, maximally spaced distribution
 * @author isaac
 */
public class DistributionFlat extends Distribution{
	private double slope;

	public DistributionFlat(int rack_size, int max_card) {
		super(rack_size, max_card);
		slope = (max_card-1) / (double) rack_size;
	}
	
	@Override
	public double eval(double x) {
		return slope*x + 1;
	}
}
