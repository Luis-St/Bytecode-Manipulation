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
	
	
	
	
	
}
