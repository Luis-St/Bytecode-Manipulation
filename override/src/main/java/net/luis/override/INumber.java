package net.luis.override;

/**
 *
 * @author Luis
 *
 */

public interface INumber {
	
	private Number self() {
		return (Number) this;
	}
	
	default double pow(double exponent) {
		return Math.pow(self().doubleValue(), exponent);
	}
	
	default double sqrt() {
		return Math.sqrt(self().doubleValue());
	}
	
}
