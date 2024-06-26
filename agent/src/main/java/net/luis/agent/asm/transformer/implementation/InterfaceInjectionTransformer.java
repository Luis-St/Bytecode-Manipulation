package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.ClassType;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionTransformer extends BaseClassTransformer {
	
	private static final String REPORT_CATEGORY = "Interface Injection Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	@SuppressWarnings("UnqualifiedFieldAccess")
	public @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ContextBasedClassVisitor(writer, type, () -> this.modified = true) {
			
			@Override
			public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
				if (lookup.containsKey(name)) {
					ClassType classType = ClassType.fromAccess(access);
					List<String> injects = lookup.getOrDefault(name, new ArrayList<>());
					if (classType == ClassType.ANNOTATION) {
						throw CrashReport.create("Cannot inject interfaces into an annotation class", REPORT_CATEGORY).addDetail("Interfaces", injects).exception();
					} else if (classType == ClassType.INTERFACE) {
						throw CrashReport.create("Cannot inject interfaces into an interface class", REPORT_CATEGORY).addDetail("Interfaces", injects).exception();
					}
					interfaces = Stream.concat(Utils.stream(interfaces), injects.stream()).distinct().toArray(String[]::new);
					this.updateClass(injects.stream().map(Type::getObjectType).toList());
					this.markModified();
				}
				super.visit(version, access, name, signature, superClass, interfaces);
			}
			
			private void updateClass(@NotNull List<Type> injects) {
				Agent.getClass(this.type).getInterfaces().addAll(injects);
			}
		};
	}
}
