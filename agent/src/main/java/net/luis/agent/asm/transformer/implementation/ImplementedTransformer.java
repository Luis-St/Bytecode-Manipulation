package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.transformer.implementation.InterfaceTransformer.InterfaceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.Map;

import static net.luis.agent.asm.Types.*;

public class ImplementedTransformer extends BaseClassTransformer {

	private final Map</*Target Class*/String, /*Interfaces*/List<InterfaceInfo>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new ImplementedVisitor(writer, type, () -> this.modified = true, this.lookup);
	}

	private static class ImplementedVisitor extends ContextBasedClassVisitor {

		private static final String REPORT_CATEGORY = "Method Implementation Error";

		private final Map</*Target Class*/String, /*Interfaces*/List<InterfaceInfo>> lookup;

		private ImplementedVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map<String, List<InterfaceInfo>> lookup) {
			super(writer, type, markModified);
			this.lookup = lookup;
		}

		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}

		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				ClassNode targetClass = Agent.getClass(Type.getObjectType(name));
				if (targetClass == null) {
					return;
				}

				for (InterfaceInfo ifaceInfo : this.lookup.get(name)) {
					ClassNode ifaceClass = ifaceInfo.classNode;
					for (MethodNode method : ifaceClass.methods) {
						if (ASMTreeUtils.hasAnnotation(method, IMPLEMENTED)) {
							this.validateMethod(method, targetClass, ifaceInfo.type);
						} else if (ASMTreeUtils.is(method, TypeAccess.PUBLIC)) {
							if (!hasAnyAnnotation(method)) {
								throw createReport("Found method without annotation, does not know how to implement", ifaceInfo.type, getDebugSignature(method)).exception();
							} else if (!hasValidAnnotation(method)) {
								throw createReport("Found method without valid annotation, does not know how to implement", ifaceInfo.type, getDebugSignature(method)).exception();
							}
						}
					}
				}
			}
		}

		protected void validateMethod(@NotNull MethodNode ifaceMethod, @NotNull ClassNode targetClass, @NotNull Type ifaceType) {
			String signature = getDebugSignature(ifaceMethod);
			if (!ASMTreeUtils.is(ifaceMethod, TypeAccess.PUBLIC)) {
				throw createReport("Method annotated with @Implemented must be public", ifaceType, signature).exception();
			}
			if (ASMTreeUtils.is(ifaceMethod, TypeModifier.STATIC)) {
				throw createReport("Method annotated with @Implemented must not be static", ifaceType, signature).exception();
			}
			if (!ASMTreeUtils.is(ifaceMethod, TypeModifier.ABSTRACT)) {
				throw createReport("Method annotated with @Implemented must not be default implemented", ifaceType, signature).exception();
			}
			MethodNode targetMethod = ASMTreeUtils.getMethod(targetClass, ifaceMethod.name + ifaceMethod.desc);
			if (targetMethod == null) {
				throw createReport("Method annotated with @Implemented must be implemented in target class", ifaceType, signature)
					.addDetailBefore("Interface", "Target Class", ASMTreeUtils.getType(targetClass)).exception();
			}
			if (!ASMTreeUtils.is(targetMethod, TypeAccess.PUBLIC)) {
				throw createReport("Method annotated with @Implemented must be public in target class", ifaceType, signature)
					.addDetailBefore("Interface", "Target Class", ASMTreeUtils.getType(targetClass))
					.addDetailBefore("Interface", "Target Method", getDebugSignature(targetMethod)).exception();
			}
		}

		//region Helper methods
		private static boolean hasAnyAnnotation(@NotNull MethodNode method) {
			return ASMTreeUtils.getAllAnnotations(method).size() > 0;
		}

		private static boolean hasValidAnnotation(@NotNull MethodNode method) {
			for (AnnotationNode annotation : ASMTreeUtils.getAllAnnotations(method)) {
				Type annotationType = Type.getType(annotation.desc);
				if (IMPLEMENTATION_ANNOTATIONS.contains(annotationType)) {
					return true;
				}
			}
			return false;
		}

		private static @NotNull String getDebugSignature(@NotNull MethodNode method) {
			Type[] paramTypes = ASMTreeUtils.getParameterTypes(method);
			StringBuilder params = new StringBuilder("(");
			for (int i = 0; i < paramTypes.length; i++) {
				if (i > 0) {
					params.append(", ");
				}
				params.append(Types.getSimpleName(paramTypes[i]));
			}
			params.append(")");
			return method.name + params;
		}
		//endregion
	}
}
