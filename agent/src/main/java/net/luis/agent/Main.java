package net.luis.agent;

import net.luis.agent.asm.generation.GenerationLoader;
import net.luis.agent.asm.generation.generators.MemorizedSupplierGenerator;
import net.luis.agent.asm.generation.generators.RuntimeUtilsGenerator;
import net.luis.agent.asm.generation.generators.concurrent.*;
import net.luis.agent.asm.transformer.*;
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
	
	// Transformers registered first will be called first, but changes will maybe overwrite by later transformers
	private static void initializeTransformers(@NotNull Instrumentation inst) {
		inst.addTransformer(new ScheduledTransformer()); // 3: Schedule
		inst.addTransformer(new AsyncTransformer()); // 2: Wrap in async
		inst.addTransformer(new CaughtTransformer()); // 1: Wrap in try-catch
		
		inst.addTransformer(new PatternTransformer()); // 4: Check pattern
		inst.addTransformer(new StringTransformer()); // 3: Modify/check string
		inst.addTransformer(new NotNullTransformer()); // 2: Throw if null
		inst.addTransformer(new DefaultTransformer()); // 1: Ensure not null
		
		inst.addTransformer(new RangeTransformer());
		
		inst.addTransformer(new RestrictedAccessTransformer());
	}
	//endregion
}