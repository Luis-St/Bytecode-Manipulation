package net.luis.asm.transformer.implementation;

import net.luis.annotation.InjectInterface;
import net.luis.asm.ASMUtils;
import net.luis.asm.base.BaseClassTransformer;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.asm.report.CrashReport;
import net.luis.preload.PreloadContext;
import net.luis.preload.data.*;
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

abstract class AbstractImplementationTransformer extends BaseClassTransformer {
	
	private static final Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	protected final PreloadContext context;
	protected final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
	
	protected AbstractImplementationTransformer(@NotNull PreloadContext context) {
		this.context = context;
		this.lookup = ASMUtils.createTargetsLookup(context, INJECT_INTERFACE);
	}
	
	@Override
	protected abstract @NotNull AbstractImplementationTransformer.ImplementationVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer);
	
	@SuppressWarnings("ProtectedInnerClass")
	protected abstract static class ImplementationVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Method Implementation Error";
		private static final Set<Type> ANNOTATIONS = Set.of(ImplementationVisitor.IMPLEMENTED, ImplementationVisitor.ACCESSOR, ImplementationVisitor.ASSIGNOR, ImplementationVisitor.INVOKER);
		
		protected static final Type IMPLEMENTED = Type.getType("Lnet/luis/annotation/Implemented;");
		protected static final Type ACCESSOR = Type.getType("Lnet/luis/annotation/Accessor;");
		protected static final Type ASSIGNOR = Type.getType("Lnet/luis/annotation/Assignor;");
		protected static final Type INVOKER = Type.getType("Lnet/luis/annotation/Invoker;");
		
		protected final PreloadContext context;
		protected final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		protected ImplementationVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer);
			this.context = context;
			this.lookup = lookup;
		}
		
		protected static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
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
						if (method.isAnnotatedWith(this.getAnnotationType())) {
							this.validateMethod(iface, method, target, targetContent);
						} else if (method.access() == TypeAccess.PUBLIC && method.is(TypeModifier.ABSTRACT)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							} else if (method.getAnnotations().stream().map(AnnotationData::type).noneMatch(ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							}
						}
					}
				}
			}
		}
		
		protected abstract @NotNull Type getAnnotationType();
		
		protected abstract void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent);
		
		protected void baseValidation(@NotNull String annotation, @NotNull Type iface, @NotNull MethodData ifaceMethod) {
			String signature = ifaceMethod.getMethodSignature();
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw createReport("Method annotated with " + annotation + " must be public", iface, signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw createReport("Method annotated with " + annotation + " must not be static", iface, signature).exception();
			}
			if (!ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw createReport("Method annotated with " + annotation + " must not be default implemented", iface, signature).exception();
			}
		}
	}
}
