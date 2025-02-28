package net.luis;
import net.luis.agent.annotation.DefaultConstructor;
import net.luis.agent.util.AccessModifier;

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
