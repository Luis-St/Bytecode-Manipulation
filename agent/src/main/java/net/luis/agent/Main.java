package net.luis.agent;

import net.luis.agent.asm.generation.GenerationLoader;
import net.luis.agent.asm.generation.generators.MemorizedSupplierGenerator;
import net.luis.agent.asm.generation.generators.RuntimeUtilsGenerator;
import net.luis.agent.asm.generation.generators.concurrent.*;
import net.luis.agent.asm.transformer.implementation.*;
import net.luis.agent.asm.transformer.method.*;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.lang.instrument.Instrumentation;
import java.util.*;

/**
 *
 * @author Luis
 *
 */

public class Main {
	
	public static void premain(@NotNull String agentArgs, @NotNull Instrumentation inst) {
		System.out.println("Loading agent");
		initialize(inst);
		initializeTransformers(inst);
		System.out.println("Agent loaded");
	}
	
	//region Initialization
	private static void initialize(@NotNull Instrumentation inst) {
		inst.redefineModule(ModuleLayer.boot().findModule("java.base").orElseThrow(), Set.of(), Map.of(), Map.of("java.lang", Set.of(Main.class.getModule())), Set.of(), Map.of());
		Agent.initialize(generateRuntimeClasses());
	}
	
	private static @NotNull Map<Type, byte[]> generateRuntimeClasses() {
		GenerationLoader loader = new GenerationLoader();
		Map<Type, byte[]> generated = new HashMap<>();
		loader.loadClass(generated, new RuntimeUtilsGenerator());
		loader.loadClass(generated, new DaemonThreadFactoryGenerator());
		loader.loadClass(generated, new CountingRunnableGenerator());
		loader.loadClass(generated, new CancelableRunnableGenerator());
		loader.loadClass(generated, new ContextRunnableGenerator());
		loader.loadClass(generated, new MemorizedSupplierGenerator());
		return generated;
	}
	
	private static void initializeTransformers(@NotNull Instrumentation inst) {
		inst.addTransformer(new InterfaceInjectionTransformer());
		inst.addTransformer(new ImplementedTransformer());
		inst.addTransformer(new AccessorTransformer());
		inst.addTransformer(new AssignorTransformer());
		inst.addTransformer(new InvokerTransformer());
		inst.addTransformer(new InjectTransformer());
		inst.addTransformer(new RedirectTransformer());
		inst.addTransformer(new InterfaceTransformer());
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
}