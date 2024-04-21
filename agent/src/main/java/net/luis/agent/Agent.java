package net.luis.agent;

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
		
		CONTEXT.getClassData();
		
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/AnnotationExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/ClassExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/EnumExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/InterfaceExample;"));
		ClassFileScanner.scanClass(Type.getType("Lnet/luis/RecordExample;"));
		
		/*inst.addTransformer(InterfaceInjectionTransformer.create(CONTEXT));*/
	}
}