package net.luis.agent.preload.data;

import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
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

public record MethodData(@NotNull String name, @NotNull Type type, @Nullable String signature, @NotNull TypeAccess access, @NotNull Set<TypeModifier> modifiers, @NotNull Map<Type, AnnotationData> annotations,
						 @NotNull List<ParameterData> parameters, @NotNull List<Type> exceptions, @NotNull Mutable<Object> annotationDefault) implements ASMData {
	
	public @NotNull String getMethodSignature() {
		return this.name + this.type;
	}
	
	public boolean isConstructor() {
		return "<init>".equals(this.name);
	}
	
	public boolean isStaticInitializer() {
		return "<clinit>".equals(this.name);
	}
	
	public boolean isMethod() {
		return !this.isConstructor() && !this.isStaticInitializer();
	}
	
	public @NotNull Type getReturnType() {
		return this.type.getReturnType();
	}
	
	public @NotNull Type getParameterType(int index) {
		return this.parameters.get(index).type();
	}
	
	public int getParameterCount() {
		return this.parameters.size();
	}
	
	public int getExceptionCount() {
		return this.exceptions.size();
	}
}
