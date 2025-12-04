package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMTreeUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.MethodOnlyClassVisitor;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.*;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceTransformer extends BaseClassTransformer {

	//region Lookup creation
	public static @NotNull Map</*Target Class*/String, /*Interfaces*/List<InterfaceInfo>> createLookup(@NotNull Type annotationType) {
		Map<String, List<InterfaceInfo>> lookup = new HashMap<>();
		Agent.stream().filter(clazz -> ASMTreeUtils.hasAnnotation(clazz, annotationType)).forEach(clazz -> {
			Type target = getTarget(clazz, annotationType);
			if (target != null) {
				InterfaceInfo info = new InterfaceInfo(ASMTreeUtils.getType(clazz), clazz);
				lookup.computeIfAbsent(target.getInternalName(), k -> new ArrayList<>()).add(info);
			}
		});
		return lookup;
	}

	private static @Nullable Type getTarget(@NotNull ClassNode clazz, @NotNull Type annotationType) {
		AnnotationNode annotation = ASMTreeUtils.getAnnotation(clazz, annotationType);
		if (annotation == null) {
			return null;
		}

		Object valueObj = ASMTreeUtils.getAnnotationValue(annotation, "value");
		org.objectweb.asm.Type value = valueObj instanceof org.objectweb.asm.Type ? (org.objectweb.asm.Type) valueObj : null;
		if (value != null && !VOID.equals(value)) {
			return value;
		}
		Object targetObj = ASMTreeUtils.getAnnotationValue(annotation, "target");
		String target = targetObj instanceof String ? (String) targetObj : null;
		if (target == null || target.isEmpty()) {
			return null;
		}
		Type type;
		if (target.startsWith("L") && target.endsWith(";")) {
			type = Type.getType(target);
		} else {
			if (target.contains(".")) {
				target = target.replace('.', '/');
			}
			type = Type.getObjectType(target);
		}
		return type;
	}

	public static class InterfaceInfo {
		public final Type type;
		public final ClassNode classNode;

		public InterfaceInfo(@NotNull Type type, @NotNull ClassNode classNode) {
			this.type = type;
			this.classNode = classNode;
		}
	}
	//endregion

	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		ClassNode classNode = Agent.getClass(type);
		if (classNode == null) {
			return true;
		}
		for (MethodNode method : classNode.methods) {
			if (ASMTreeUtils.hasAnnotation(method, INJECT)) {
				return false;
			}
		}
		return true;
	}
	//endregion

	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		ClassNode classNode = Agent.getClass(type);
		Type target = getTarget(classNode, INJECT_INTERFACE);
		return new InterfaceClassVisitor(writer, type, classNode, target, () -> this.modified = true);
	}

	private static class InterfaceClassVisitor extends MethodOnlyClassVisitor {

		private static final Map<Type, List<String>> ALIASES = Utils.make(new HashMap<>(), map -> {
			map.put(INJECT, List.of("inject"));
			map.put(REDIRECT, List.of("redirect"));
			map.put(MODIFY, List.of("modify"));
		});

		private final ClassNode classNode;
		private final Type target;

		private InterfaceClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull ClassNode classNode, @Nullable Type target, @NotNull Runnable markModified) {
			super(writer, type, markModified);
			this.classNode = classNode;
			this.target = target;
		}

		@Override
		protected boolean isMethodValid(@NotNull MethodNode methodNode) {
			if (this.target == null) {
				return false;
			}
			return this.isMethodValid(methodNode, INJECT) || this.isMethodValid(methodNode, REDIRECT);
		}

		@Override
		protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter mv, @NotNull MethodNode methodNode) {
			List<String> restrictedValues = new ArrayList<>();
			if (this.isMethodValid(methodNode, INJECT)) {
				restrictedValues.add(this.getRestrictedValues(methodNode, INJECT));
			}
			if (this.isMethodValid(methodNode, REDIRECT)) {
				restrictedValues.add(this.getRestrictedValues(methodNode, REDIRECT));
			}

			AnnotationVisitor av = mv.visitAnnotation(RESTRICTED_ACCESS.getDescriptor(), true);
			AnnotationVisitor array = av.visitArray("value");
			restrictedValues.forEach(value -> array.visit(null, value));
			array.visitEnd();
			av.visitEnd();

			return mv;
		}

		//region Helper methods
		private boolean isMethodValid(@NotNull MethodNode methodNode, @NotNull Type type) {
			if (!ASMTreeUtils.hasAnnotation(methodNode, type)) {
				return false;
			}
			AnnotationNode annotation = ASMTreeUtils.getAnnotation(methodNode, type);
			boolean restricted = ASMTreeUtils.getAnnotationValue(annotation, "restricted", false);
			Object lambda = ASMTreeUtils.getAnnotationValue(annotation, "lambda");
			if (restricted && lambda == null) {
				return !ASMTreeUtils.is(methodNode, TypeModifier.ABSTRACT);
			}
			return false;
		}

		private @NotNull String getRestrictedValues(@NotNull MethodNode methodNode, @NotNull Type annotation) {
			String value = this.getTarget(methodNode, annotation);
			if (value.contains("(")) {
				value = value.substring(0, value.indexOf('('));
			}
			return Objects.requireNonNull(this.target).getClassName() + "#" + value;
		}

		private @NotNull String getTarget(@NotNull MethodNode methodNode, @NotNull Type annotation) {
			AnnotationNode annotationNode = ASMTreeUtils.getAnnotation(methodNode, annotation);
			Object targetObj = ASMTreeUtils.getAnnotationValue(annotationNode, "method");
			String target = targetObj instanceof String ? (String) targetObj : null;
			if (target != null) {
				return target;
			}
			String methodName = methodNode.name;
			for (String alias : ALIASES.get(annotation)) {
				if (methodName.startsWith(alias)) {
					return Utils.uncapitalize(methodName.substring(alias.length()));
				}
			}
			return methodName;
		}
		//endregion
	}
}
