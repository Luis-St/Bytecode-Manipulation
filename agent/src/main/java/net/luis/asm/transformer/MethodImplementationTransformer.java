package net.luis.asm.transformer;

import net.luis.annotation.InjectInterface;
import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.asm.report.CrashReport;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.ClassContent;
import net.luis.preload.data.MethodData;
import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class MethodImplementationTransformer extends BaseClassTransformer {
	
	private static final Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	private final PreloadContext context;
	
	public MethodImplementationTransformer(@NotNull PreloadContext context) {
		this.context = context;
	}
	
	@Override
	protected ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new ImplementationClassVisitor(writer, this.context, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE));
	}
	
	private static class ImplementationClassVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Method Implementation Error";
		private static final Type IMPLEMENTED = Type.getType("Lnet/luis/annotation/Implemented;");
		private static final Type ACCESSOR = Type.getType("Lnet/luis/annotation/Accessor;");
		private static final Type ASSIGNOR = Type.getType("Lnet/luis/annotation/Assignor;");
		private static final Type INVOKER = Type.getType("Lnet/luis/annotation/Invoker;");
		
		private final Map</*Target Field*/String, /*Accessor Name*/String> accessors = new HashMap<>();
		private final Map</*Target Field*/String, /*Assigner Name*/String> assigners = new HashMap<>();
		private final Map</*Target Method*/String, /*Invoker Name*/String> invokers = new HashMap<>();
		private final PreloadContext context;
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private ImplementationClassVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer);
			this.context = context;
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY)
				.addDetail("Interface", iface)
				.addDetail("Interface Method", methodSignature);
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				Type target = Type.getObjectType(name);
				ClassContent targetContent = this.context.getClassContent(target);
				// Expected the target to be an interface
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassContent ifaceContent = this.context.getClassContent(iface);
					for (MethodData method : ifaceContent.methods()) {
						if (method.isAnnotatedWith(IMPLEMENTED)) {
							this.validateImplemented(iface, method, target, targetContent);
						} else if (method.isAnnotatedWith(ACCESSOR)) {
							this.addAccessor(method, target, targetContent);
						} else if (method.isAnnotatedWith(ASSIGNOR)) {
							this.addAssignor(method, target, targetContent);
						} else if (method.isAnnotatedWith(INVOKER)) {
							this.addInvoker(method, target, targetContent);
						} else if (method.access() == TypeAccess.PUBLIC && method.is(TypeModifier.ABSTRACT)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							} else {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateImplemented(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			String signature = ifaceMethod.getMethodSignature();
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw createReport("Method annotated with @Implemented must be public", iface, signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw createReport("Method annotated with @Implemented must not be static", iface, signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw createReport("Method annotated with @Implemented must not be default implemented", iface, signature).exception();
			}
			if (!targetContent.hasMethod(ifaceMethod.name(), ifaceMethod.type())) {
				throw createReport("Method annotated with @Implemented must be implemented in target class", iface, signature)
					.addDetailBefore("Interface", "Target Class", target).exception();
			}
			MethodData targetMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (targetMethod.access() != TypeAccess.PUBLIC) {
				throw createReport("Method annotated with @Implemented must be public in target class", iface, signature)
					.addDetailBefore("Interface", "Target Class", target)
					.addDetailBefore("Interface", "Target Method", targetMethod.getMethodSignature()).exception();
			}
		}
		
		private void addAccessor(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
		
		}
		
		private void addAssignor(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
		
		}
		
		private void addInvoker(@NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
		
		}
	}
}
