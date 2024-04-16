package net.luis.agent;

import net.luis.asm.AnnotationScanTransformer;
import net.luis.preload.Preloader;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		System.out.println("Agent loaded");
		//inst.addTransformer(new AnnotationScanTransformer(), true);
	}
	
	static {
		Preloader preloader = new Preloader();
		preloader.preload();
	}
}