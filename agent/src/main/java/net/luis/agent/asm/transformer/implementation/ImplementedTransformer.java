package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.AgentContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

import static net.luis.agent.asm.Types.*;

public class ImplementedTransformer extends BaseClassTransformer {
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(AgentContext.get(), INJECT_INTERFACE);
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new ImplementedVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class ImplementedVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Method Implementation Error";
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private ImplementedVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		@SuppressWarnings("DuplicatedCode")
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				AgentContext context = AgentContext.get();
				Type target = Type.getObjectType(name);
				ClassData targetData = context.getClassData(target);
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassData ifaceData = context.getClassData(iface);
					for (MethodData method : ifaceData.methods()) {
						if (method.isAnnotatedWith(IMPLEMENTED)) {
							this.validateMethod(iface, method, target, targetData);
						} else if (method.is(TypeAccess.PUBLIC)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							} else if (method.getAnnotations().stream().map(AnnotationData::type).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							}
						}
					}
				}
			}
		}
		
		protected void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassData targetData) {
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw createReport("Method annotated with @Implemented must be public", iface, signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw createReport("Method annotated with @Implemented must not be static", iface, signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw createReport("Method annotated with @Implemented must not be default implemented", iface, signature).exception();
			}
			//endregion
			MethodData targetMethod = targetData.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (targetMethod == null) {
				throw createReport("Method annotated with @Implemented must be implemented in target class", iface, signature)
					.addDetailBefore("Interface", "Target Class", target).exception();
			}
			if (targetMethod.access() != TypeAccess.PUBLIC) {
				throw createReport("Method annotated with @Implemented must be public in target class", iface, signature)
					.addDetailBefore("Interface", "Target Class", target)
					.addDetailBefore("Interface", "Target Method", targetMethod.getMethodSignature()).exception();
			}
		}
	}
}
