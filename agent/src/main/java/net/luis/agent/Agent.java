package net.luis.agent;

import net.luis.asm.AnnotationScanTransformer;
import net.luis.preload.PreloadContext;
import net.luis.preload.Preloader;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Map;

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
		PreloadContext context = Preloader.preload();
		
		for (Map.Entry<String, Map<String, Object>> entry : context.getClassAnnotations().entrySet()) {
			System.out.println(entry.getKey() + " -> " + entry.getValue());
		}
	}
}