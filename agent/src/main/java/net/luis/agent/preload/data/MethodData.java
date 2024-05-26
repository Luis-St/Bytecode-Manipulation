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
						 @NotNull Map<Type, AnnotationData> annotations, @NotNull List<ParameterData> parameters, @NotNull List<Type> exceptions, @NotNull Map<Integer, LocalVariableData> localVariables,
						 @NotNull Mutable<Object> annotationDefault) implements ASMData {
	
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
	
	public @Nullable LocalVariableData getLocalVariable(int index) {
		return this.localVariables.get(index);
	}
	
	//region Copy
	public @NotNull MethodData copy(@NotNull String name) {
		return new MethodData(this.owner, name, this.type, this.signature, this.access, this.methodType, EnumSet.copyOf(this.modifiers), new HashMap<>(this.annotations), new ArrayList<>(this.parameters),
			new ArrayList<>(this.exceptions), new HashMap<>(this.localVariables), new Mutable<>());
	}
	
	public @NotNull MethodData copy(@NotNull Type type, @NotNull List<ParameterData> parameters) {
		return new MethodData(this.owner, this.name, type, this.signature, this.access, this.methodType, EnumSet.copyOf(this.modifiers), new HashMap<>(this.annotations), parameters,
			new ArrayList<>(this.exceptions), new HashMap<>(this.localVariables), new Mutable<>());
	}
	
	public @NotNull MethodData copy(@NotNull TypeAccess access) {
		return new MethodData(this.owner, this.name, this.type, this.signature, access, this.methodType, EnumSet.copyOf(this.modifiers), new HashMap<>(this.annotations), new ArrayList<>(this.parameters),
			new ArrayList<>(this.exceptions), new HashMap<>(this.localVariables), new Mutable<>());
	}
	
	public @NotNull MethodData copy(@NotNull Set<TypeModifier> modifiers) {
		return new MethodData(this.owner, this.name, this.type, this.signature, this.access, this.methodType, modifiers, new HashMap<>(this.annotations), new ArrayList<>(this.parameters),
			new ArrayList<>(this.exceptions), new HashMap<>(this.localVariables), new Mutable<>());
	}
	
	public @NotNull MethodData copy(@NotNull Map<Type, AnnotationData> annotations) {
		return new MethodData(this.owner, this.name, this.type, this.signature, this.access, this.methodType, EnumSet.copyOf(this.modifiers), annotations, new ArrayList<>(this.parameters),
			new ArrayList<>(this.exceptions), new HashMap<>(this.localVariables), new Mutable<>());
	}
	//endregion
}
