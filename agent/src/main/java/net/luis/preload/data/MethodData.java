package net.luis.preload.data;

import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
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
						 @NotNull List<ParameterData> parameters, @NotNull List<Type> exceptions) implements ASMData {
	
	public @NotNull String getMethodSignature() {
		return this.name + this.type;
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
