package net.luis.agent;

import net.luis.asm.InterfaceInjectionScanner;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		System.out.println("Agent loaded!");
		inst.addTransformer(new InterfaceInjectionScanner(inst), true);
		
		
		
	}
}