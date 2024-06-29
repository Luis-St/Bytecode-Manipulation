package net.luis.agent.asm.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.Type;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public class ActualType {
	
	private final Type type;
	private final List<ActualType> nested;
	
	private ActualType(@NotNull Type type, @NotNull List<ActualType> nested) {
		this.type = type;
		this.nested = nested;
	}
	
	//region Factory methods
	public static @NotNull ActualType of(@NotNull Type type) {
		return new ActualType(type, new LinkedList<>());
	}
	
	public static @NotNull ActualType of(@NotNull Type type, @NotNull List<ActualType> nested) {
		return new ActualType(type, nested);
	}
	
	public static @NotNull ActualType flatDown(@NotNull ActualType declaration) {
		return flatDown("", declaration);
	}
	
	public static @NotNull ActualType flatDown(@NotNull String pre, @NotNull ActualType declaration) {
		return new ActualType(Type.getType(pre + declaration.type().getDescriptor()), new LinkedList<>());
	}
	//endregion
	
	public @NotNull Type type() {
		return this.type;
	}
	
	@Unmodifiable
	public @NotNull List<ActualType> nested() {
		return List.copyOf(this.nested);
	}
}
