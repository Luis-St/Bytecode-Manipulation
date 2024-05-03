package net.luis.asm.transformer;

import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.asm.report.CrashReport;
import net.luis.preload.PreloadContext;
import net.luis.preload.type.ClassType;
import net.luis.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Stream;

import static net.luis.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceInjectionTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
	
	public InterfaceInjectionTransformer(@NotNull PreloadContext context) {
		this.lookup = ASMUtils.createTargetsLookup(context, INJECT_INTERFACE);
	}
	
	@Override
	@SuppressWarnings({ "UnqualifiedFieldAccess", "ReturnOfInnerClass" })
	public @NotNull ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new BaseClassVisitor(writer) {
			private static final String REPORT_CATEGORY = "Interface Injection Error";
			
			@Override
			public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
				if (lookup.containsKey(name)) {
					ClassType type = ClassType.fromAccess(access);
					List<String> injects = lookup.getOrDefault(name, new ArrayList<>());
					if (type == ClassType.ANNOTATION) {
						throw CrashReport.create("Cannot inject interfaces into an annotation class", REPORT_CATEGORY).addDetail("Interfaces", injects).exception();
					} else if (type == ClassType.INTERFACE) {
						throw CrashReport.create("Cannot inject interfaces into an interface class", REPORT_CATEGORY).addDetail("Interfaces", injects).exception();
					}
					interfaces = Stream.concat(Utils.stream(interfaces), injects.stream()).distinct().toArray(String[]::new);
					modified = true;
				}
				super.visit(version, access, name, signature, superClass, interfaces);
			}
		};
	}
}
