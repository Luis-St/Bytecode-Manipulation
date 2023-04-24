package net.luis.agent;

import java.lang.instrument.Instrumentation;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	public static void premain(String args, Instrumentation inst) {
		System.out.println("Agent.premain() called");
	}
}
