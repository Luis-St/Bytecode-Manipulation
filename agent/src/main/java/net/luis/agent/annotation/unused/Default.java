package net.luis.agent.annotation.unused;

/**
 *
 * @author Luis-St
 *
 */

public @interface Default {
	
	String value();
	
	Class<? extends StringFactory<?>> factory();
}
