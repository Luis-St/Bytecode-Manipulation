package net.luis;

import net.luis.annotations.DefaultConstructor;
import net.luis.annotations.configuration.AccessModifier;

/**
 *
 * @author Luis-St
 *
 */

@DefaultConstructor(AccessModifier.PRIVATE)
public class Test {
	
	public static final Test INSTANCE = new Test();
	
	@Override
	public String toString() {
		return "Test";
	}
}
