package net.luis.agent;

import net.luis.agent.asm.transformer.implementation.*;
import net.luis.agent.asm.transformer.method.*;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	public static void premain(@NotNull String agentArgs, @NotNull Instrumentation inst) {
		System.out.println("Agent loaded");
		AgentContext.get().initialize();
		inst.addTransformer(new InterfaceTransformer());
		inst.addTransformer(new InterfaceInjectionTransformer());
		inst.addTransformer(new ImplementedTransformer());
		inst.addTransformer(new AccessorTransformer());
		inst.addTransformer(new AssignorTransformer());
		inst.addTransformer(new InvokerTransformer());
		inst.addTransformer(new InjectorTransformer());
		inst.addTransformer(new ScheduledTransformer());
		inst.addTransformer(new CaughtTransformer());
		inst.addTransformer(new AsyncTransformer());
		inst.addTransformer(new PatternTransformer());
		inst.addTransformer(new NotNullTransformer());
		inst.addTransformer(new DefaultTransformer());
		inst.addTransformer(new RangeTransformer());
		inst.addTransformer(new RestrictedAccessTransformer());
	}
}