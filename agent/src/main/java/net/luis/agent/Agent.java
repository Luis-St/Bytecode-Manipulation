package net.luis.agent;

import net.luis.asm.transformer.InterfaceInjectionTransformer;
import net.luis.asm.transformer.implementation.ImplementedValidationTransformer;
import net.luis.preload.ClassFileScanner;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.ClassContent;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.instrument.Instrumentation;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	private static final PreloadContext CONTEXT = new PreloadContext();
	
	public static void premain(@NotNull String agentArgs, @NotNull Instrumentation inst) {
		System.out.println("Agent loaded");
		//ClassFileScanner.scanClass(Type.getType("Lnet/luis/Main;"));
		ClassContent content = ClassFileScanner.scanClassContent(Type.getType("Lnet/luis/MyInterface;"));
		content.getFields().forEach(field -> System.out.println(field.type()));
		content.methods().forEach(method -> System.out.println(method.parameters()));
		
		inst.addTransformer(new InterfaceInjectionTransformer(CONTEXT));
		inst.addTransformer(new ImplementedValidationTransformer(CONTEXT));
	}
}