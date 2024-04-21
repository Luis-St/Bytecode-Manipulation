package net.luis.agent;

import net.luis.asm.transformer.InterfaceInjectionTransformer;
import net.luis.preload.ClassFileScanner;
import net.luis.preload.PreloadContext;
import org.objectweb.asm.Type;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	private static final PreloadContext CONTEXT = new PreloadContext();
	
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		System.out.println("Agent loaded");
		inst.addTransformer(InterfaceInjectionTransformer.create(CONTEXT));
	}
}