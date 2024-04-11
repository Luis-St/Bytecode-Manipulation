package net.luis.overrides;

/**
 *
 * @author Luis
 *
 */

public interface ITestInterface {
	
	default double pow(double exponent) {
		return Math.pow(this.hashCode(), exponent);
	}
	
	default double sqrt() {
		return Math.sqrt(this.hashCode());
	}
}
