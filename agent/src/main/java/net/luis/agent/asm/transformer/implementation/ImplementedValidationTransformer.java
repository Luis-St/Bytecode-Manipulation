package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

import static net.luis.agent.asm.Types.*;

public class ImplementedValidationTransformer extends BaseClassTransformer {
	
	private final PreloadContext context;
	
	public ImplementedValidationTransformer(@NotNull PreloadContext context) {
		this.context = context;
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new ImplementedValidationVisitor(writer, this.context, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE));
	}
	
	private static class ImplementedValidationVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Method Implementation Error";
		
		private final PreloadContext context;
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private ImplementedValidationVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer);
			this.context = context;
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
				Type target = Type.getObjectType(name);
				ClassContent targetContent = this.context.getClassContent(target);
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassContent ifaceContent = this.context.getClassContent(iface);
					for (MethodData method : ifaceContent.methods()) {
						if (method.isAnnotatedWith(IMPLEMENTED)) {
							this.validateMethod(iface, method, target, targetContent);
						} else if (method.access() == TypeAccess.PUBLIC && method.is(TypeModifier.ABSTRACT)) {
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
		
		protected void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			//System.out.println("Validating Implemented - " + ifaceMethod.name() + " - " + iface.getInternalName());
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
	}
}
