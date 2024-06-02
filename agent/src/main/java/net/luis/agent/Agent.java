package net.luis.agent;

import net.luis.agent.asm.generation.GenerationLoader;
import net.luis.agent.asm.generation.RuntimeUtilsGenerator;
import net.luis.agent.asm.transformer.implementation.*;
import net.luis.agent.asm.transformer.method.*;
import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Luis
 *
 */

public class Agent {
	
	public static void premain(@NotNull String agentArgs, @NotNull Instrumentation inst) {
		System.out.println("Agent loaded");
		initialize(inst);
		initializeTransformers(inst);
	}
	
	//region Initialization
	private static void initialize(@NotNull Instrumentation inst) {
		AgentContext.get().initialize(inst);
		inst.redefineModule(ModuleLayer.boot().findModule("java.base").orElseThrow(), Set.of(), Map.of(), Map.of("java.lang", Set.of(Agent.class.getModule())), Set.of(), Map.of());
		generateRuntimeClasses();
	}
	
	private static void initializeTransformers(@NotNull Instrumentation inst) {
		inst.addTransformer(new InterfaceTransformer());
		inst.addTransformer(new InterfaceInjectionTransformer());
		inst.addTransformer(new ImplementedTransformer());
		inst.addTransformer(new AccessorTransformer());
		inst.addTransformer(new AssignorTransformer());
		inst.addTransformer(new InvokerTransformer());
		inst.addTransformer(new InjectorTransformer());
		inst.addTransformer(new RedirectorTransformer());
		inst.addTransformer(new ScheduledTransformer());
		inst.addTransformer(new CaughtTransformer());
		inst.addTransformer(new AsyncTransformer());
		inst.addTransformer(new PatternTransformer());
		inst.addTransformer(new NotNullTransformer());
		inst.addTransformer(new DefaultTransformer());
		inst.addTransformer(new RangeTransformer());
		inst.addTransformer(new RestrictedAccessTransformer());
	}
	//endregion
	
	private static void generateRuntimeClasses() {
		GenerationLoader loader = new GenerationLoader();
		loader.loadClass(new RuntimeUtilsGenerator());
	}
}