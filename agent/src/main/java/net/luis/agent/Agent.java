package net.luis.agent;

import net.luis.preload.PreloadContext;

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
		/*inst.addTransformer(InterfaceInjectionTransformer.create(CONTEXT));*/
		/*List<Class<?>> targets = CONTEXT.getClassAnnotation("net.luis.MyInterface", "Lnet/luis/annotation/InterfaceInjection;").get("targets");
		System.out.println(targets);*/
	}
	
}