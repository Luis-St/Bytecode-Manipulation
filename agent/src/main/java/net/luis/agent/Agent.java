package net.luis.agent;

import net.luis.asm.AnnotationScanTransformer;
import net.luis.preload.PreloadContext;
import net.luis.preload.Preloader;
import net.luis.preload.data.AnnotationData;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	private static final PreloadContext CONTEXT = Preloader.preload();
	
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		System.out.println("Agent loaded");
	}
	
	static {
		for (Map.Entry<String, List<AnnotationData>> entry : CONTEXT.getClassAnnotations().entrySet()) {
			System.out.println(entry.getKey());
			for (AnnotationData data : entry.getValue()) {
				System.out.println("  " + data.name());
				data.values().forEach((key, value) -> System.out.println("    " + key + " = " + value));
			}
		}
	}
}