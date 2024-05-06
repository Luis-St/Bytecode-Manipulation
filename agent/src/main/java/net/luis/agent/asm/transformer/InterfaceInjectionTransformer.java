package net.luis.agent.asm.transformer;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.type.ClassType;
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
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
	
	public InterfaceInjectionTransformer(@NotNull PreloadContext context) {
		super(context);
		this.lookup = ASMUtils.createTargetsLookup(context, INJECT_INTERFACE);
	}
	
	@Override
	@SuppressWarnings("UnqualifiedFieldAccess")
	public @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new BaseClassVisitor(writer) {
			private static final String REPORT_CATEGORY = "Interface Injection Error";
			
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
					modified = true;
				}
				super.visit(version, access, name, signature, superClass, interfaces);
			}
			
			private void updateClass(@NotNull List<Type> injects) {
				context.getClassInfo(type).interfaces().addAll(injects);
			}
		};
	}
}
