package net.luis.agent.asm.type;

/**
 *
 * @author Luis-St
 *
 */

public enum MethodType {
	
	CONSTRUCTOR, PRIMARY_CONSTRUCTOR, STATIC_INITIALIZER, METHOD;
	
	public static MethodType fromName(String name) {
		if ("<init>".equalsIgnoreCase(name)) {
			return CONSTRUCTOR;
		} else if ("<clinit>".equalsIgnoreCase(name)) {
			return STATIC_INITIALIZER;
		} else {
			return METHOD;
		}
	}
}
