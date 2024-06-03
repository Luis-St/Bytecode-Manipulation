package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.MethodOnlyClassVisitor;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.ArrayList;
import java.util.List;

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
		Class clazz = AgentContext.get().getClass(type);
		return clazz.getMethods().values().stream().noneMatch(method -> method.isAnnotatedWith(INJECTOR));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		List<Type> targets = AgentContext.get().getClass(type).getAnnotation(INJECT_INTERFACE).get("targets");
		return new InterfaceClassVisitor(writer, type, targets == null ? new ArrayList<>() : targets, () -> this.modified = true);
	}
	
	private static class InterfaceClassVisitor extends MethodOnlyClassVisitor {
		
		private final List<Type> targets;
		
		private InterfaceClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull List<Type> targets, @NotNull Runnable markModified) {
			super(writer, type, markModified);
			this.targets = targets;
		}
		
		@Override
		protected boolean isMethodValid(@NotNull Method method) {
			if (method.isAnnotatedWith(INJECTOR)) {
				Annotation annotation = method.getAnnotation(INJECTOR);
				if (annotation.getOrDefault("restricted")) {
					return super.isMethodValid(method);
				}
			}
			return false;
		}
		
		@Override
		protected @NotNull MethodVisitor createMethodVisitor(@NotNull LocalVariablesSorter mv, @NotNull Method method) {
			List<String> restrictedValues = this.getRestrictedValues(method);
			
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
		private @NotNull List<String> getRestrictedValues(@NotNull Method method) {
			String invokerTarget = this.getTarget(method);
			if (invokerTarget.contains("(")) {
				invokerTarget = invokerTarget.substring(0, invokerTarget.indexOf('('));
			}
			List<String> values = new ArrayList<>();
			for (Type target : this.targets) {
				values.add(target.getClassName() + "#" + invokerTarget);
			}
			return values;
		}
		
		private @NotNull String getTarget(@NotNull Method method) {
			Annotation annotation = method.getAnnotation(INJECTOR);
			String target = annotation.get("method");
			if (target != null) {
				return target;
			}
			String methodName = method.getName();
			if (methodName.startsWith("inject")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		//endregion
	}
}
