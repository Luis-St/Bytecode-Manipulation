package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.MethodOnlyClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
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
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		Class clazz = Agent.getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(INJECTOR));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		List<Type> targets = Agent.getClass(type).getAnnotation(INJECT_INTERFACE).get("targets");
		return new InterfaceClassVisitor(writer, type, targets == null ? new ArrayList<>() : targets, () -> this.modified = true);
	}
	
	private static class InterfaceClassVisitor extends MethodOnlyClassVisitor {
		
		private static final Map<Type, List<String>> ALIASES = Utils.make(new HashMap<>(), map -> {
			map.put(INJECTOR, List.of("inject"));
			map.put(REDIRECTOR, List.of("redirect"));
		});
		
		private final List<Type> targets;
		
		private InterfaceClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull List<Type> targets, @NotNull Runnable markModified) {
			super(writer, type, markModified);
			this.targets = targets;
		}
		
		@Override
		protected boolean isMethodValid(@NotNull Method method) {
			return this.isMethodValid(method, INJECTOR) || this.isMethodValid(method, REDIRECTOR);
		}
		
		@Override
		protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter mv, @NotNull Method method) {
			List<String> restrictedValues = new ArrayList<>();
			if (this.isMethodValid(method, INJECTOR)) {
				restrictedValues = this.getRestrictedValues(method, INJECTOR);
			} else if (this.isMethodValid(method, REDIRECTOR)) {
				restrictedValues = this.getRestrictedValues(method, REDIRECTOR);
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
		private boolean isMethodValid(@NotNull Method method, @NotNull Type annotation) {
			if (method.isAnnotatedWith(annotation)) {
				if (method.getAnnotation(annotation).getOrDefault("restricted")) {
					return super.isMethodValid(method);
				}
			}
			return false;
		}
		
		private @NotNull List<String> getRestrictedValues(@NotNull Method method, @NotNull Type annotation) {
			String value = this.getTarget(method, annotation);
			if (value.contains("(")) {
				value = value.substring(0, value.indexOf('('));
			}
			List<String> values = new ArrayList<>();
			for (Type target : this.targets) {
				values.add(target.getClassName() + "#" + value);
			}
			return values;
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
