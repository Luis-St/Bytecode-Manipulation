package net.luis.agent.preload.data;

import net.luis.agent.preload.type.*;
import net.luis.agent.util.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public record MethodData(@NotNull Type owner, @NotNull String name, @NotNull Type type, @Nullable String signature, @NotNull TypeAccess access, @NotNull MethodType methodType, @NotNull Set<TypeModifier> modifiers,
						 @NotNull Map<Type, AnnotationData> annotations, @NotNull List<ParameterData> parameters, @NotNull List<Type> exceptions, @NotNull Mutable<Object> annotationDefault) implements ASMData {
	
	public @NotNull String getMethodSignature() {
		return this.name + this.type;
	}
	
	public boolean is(MethodType type) {
		return this.methodType == type;
	}
	
	public boolean isImplementedMethod() {
		return this.is(MethodType.METHOD) && !this.is(TypeModifier.ABSTRACT);
	}
	
	public boolean returns(@NotNull Type type) {
		return this.getReturnType().equals(type);
	}
	
	public boolean returnsAny(@NotNull Type... types) {
		return Arrays.stream(types).anyMatch(this::returns);
	}
	
	//region Type getters
	public @NotNull Type getReturnType() {
		return this.type.getReturnType();
	}
	
	public @NotNull Type getParameterType(int index) {
		return this.parameters.get(index).type();
	}
	//endregion
	
	public int getParameterCount() {
		return this.parameters.size();
	}
	
	public int getExceptionCount() {
		return this.exceptions.size();
	}
}
