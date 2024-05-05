package net.luis.agent;

import net.luis.agent.asm.transformer.*;
import net.luis.agent.asm.transformer.implementation.*;
import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;

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
		//ClassContent content = ClassFileScanner.scanClassContent(Type.getType("Lnet/luis/ClassExample;"));
		//ClassContent content = ClassFileScanner.scanClassContent(Type.getType("Lnet/luis/MyInterface;"));
		//ClassContent content = ClassFileScanner.scanClassContent(Type.getType("Lnet/luis/Test;"));
		//content.getFields().forEach(field -> System.out.println(field.name() + ": " + field.signature()));
		//content.methods().forEach(method -> System.out.println(method.name() + ": " + method.signature()));
		
		inst.addTransformer(new InterfaceInjectionTransformer(CONTEXT));
		inst.addTransformer(new ImplementedValidationTransformer(CONTEXT));
		inst.addTransformer(new AccessorImplementationTransformer(CONTEXT));
		inst.addTransformer(new AssignorImplementationTransformer(CONTEXT));
		inst.addTransformer(new InvokerImplementationTransformer(CONTEXT));
		inst.addTransformer(new PatternTransformer(CONTEXT));
		inst.addTransformer(new NotNullTransformer(CONTEXT));
		inst.addTransformer(new DefaultTransformer(CONTEXT));
	}
}