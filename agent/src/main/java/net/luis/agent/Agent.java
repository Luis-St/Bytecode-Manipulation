package net.luis.agent;

import java.lang.instrument.Instrumentation;

public class Agent {
	
	private static Instrumentation instrumentation;
	
	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("Agent.premain() called");
		instrumentation = inst;
	}
	
	public static long sizeOf(Object object) {
		return instrumentation.getObjectSize(object);
	}
}
