package net.luis.agent.preload.type;

/**
 *
 * @author Luis-St
 *
 */

public enum MethodType {
	
	CONSTRUCTOR,
	STATIC_INITIALIZER,
	METHOD;
	
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
