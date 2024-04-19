package net.luis.agent;

import net.luis.preload.PreloadContext;
import net.luis.preload.Preloader;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	private static final PreloadContext CONTEXT = Preloader.preload();
	
	public static void premain(String agentArgs, Instrumentation inst) throws UnmodifiableClassException {
		System.out.println("Agent loaded");
		/*inst.addTransformer(InterfaceInjectionTransformer.create(CONTEXT));*/
		/*List<Class<?>> targets = CONTEXT.getClassAnnotation("net.luis.MyInterface", "Lnet/luis/annotation/InterfaceInjection;").get("targets");
		System.out.println(targets);*/
	}
	
/*	static {
		for (Map.Entry<String, List<AnnotationData>> entry : CONTEXT.getClassAnnotations().entrySet()) {
			System.out.println(entry.getKey());
			for (AnnotationData data : entry.getValue()) {
				System.out.println("  " + data.descriptor());
				data.values().forEach((key, value) -> System.out.println("    " + key + " = " + value));
			}
		}
	}*/
}