package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.MethodOnlyClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InterfaceTransformer extends BaseClassTransformer {
	
	//region Lookup creation
	public static @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> createLookup(@NotNull Type annotationType) {
		Map<String, List<String>> lookup = new HashMap<>();
		Agent.stream().filter(clazz -> clazz.isAnnotatedWith(annotationType)).forEach(clazz -> {
			Type target = getTarget(clazz, annotationType);
			if (target != null) {
				lookup.computeIfAbsent(target.getInternalName(), k -> new ArrayList<>()).add(clazz.getType().getInternalName());
			}
		});
		return lookup;
	}
	
	private static @Nullable Type getTarget(@NotNull Class clazz, @NotNull Type annotationType) {
		Type value = clazz.getAnnotation(annotationType).get("value");
		if (value != null && !VOID.equals(value)) {
			return value;
		}
		String target = clazz.getAnnotation(annotationType).get("target");
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
	//endregion
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(INJECT));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		Type target = getTarget(Agent.getClass(type), INJECT_INTERFACE);
		return new InterfaceClassVisitor(writer, type, target, () -> this.modified = true);
	}
	
	private static class InterfaceClassVisitor extends MethodOnlyClassVisitor {
		
		private static final Map<Type, List<String>> ALIASES = Utils.make(new HashMap<>(), map -> {
			map.put(INJECT, List.of("inject"));
			map.put(REDIRECT, List.of("redirect"));
			map.put(MODIFY, List.of("modify"));
		});
		
		private final Type target;
		
		private InterfaceClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @Nullable Type target, @NotNull Runnable markModified) {
			super(writer, type, markModified);
			this.target = target;
		}
		
		@Override
		protected boolean isMethodValid(@NotNull Method method) {
			return this.target != null && (this.isMethodValid(method, INJECT) || this.isMethodValid(method, REDIRECT));
		}
		
		@Override
		protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter mv, @NotNull Method method) {
			List<String> restrictedValues = new ArrayList<>();
			if (this.isMethodValid(method, INJECT)) {
				restrictedValues.add(this.getRestrictedValues(method, INJECT));
			}
			if (this.isMethodValid(method, REDIRECT)) {
				restrictedValues.add(this.getRestrictedValues(method, REDIRECT));
			}
			
			AnnotationVisitor av = mv.visitAnnotation(RESTRICTED_ACCESS.getDescriptor(), true);
			AnnotationVisitor array = av.visitArray("value");
			restrictedValues.forEach(value -> array.visit(null, value));
			array.visitEnd();
			av.visitEnd();
			
			Annotation annotation = Annotation.of(RESTRICTED_ACCESS);
			annotation.getValues().put("value", restrictedValues);
			method.getAnnotations().put(RESTRICTED_ACCESS, annotation);
			return mv;
		}
		
		//region Helper methods
		private boolean isMethodValid(@NotNull Method method, @NotNull Type type) {
			if (method.isAnnotatedWith(type)) {
				Annotation annotation = method.getAnnotation(type);
				if ((boolean) annotation.getOrDefault("restricted") && !annotation.getValues().containsKey("lambda")) {
					return super.isMethodValid(method);
				}
			}
			return false;
		}
		
		private @NotNull String getRestrictedValues(@NotNull Method method, @NotNull Type annotation) {
			String value = this.getTarget(method, annotation);
			if (value.contains("(")) {
				value = value.substring(0, value.indexOf('('));
			}
			return Objects.requireNonNull(this.target).getClassName() + "#" + value;
		}
		
		private @NotNull String getTarget(@NotNull Method method, @NotNull Type annotation) {
			String target = method.getAnnotation(annotation).get("method");
			if (target != null) {
				return target;
			}
			String methodName = method.getName();
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
