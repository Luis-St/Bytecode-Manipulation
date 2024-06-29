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

public class GenericDeclaration {
	
	private final Type type;
	private final List<GenericDeclaration> nested;
	
	private GenericDeclaration(@NotNull Type type, @NotNull List<GenericDeclaration> nested) {
		this.type = type;
		this.nested = nested;
	}
	
	//region Factory methods
	public static @NotNull GenericDeclaration of(@NotNull Type type) {
		return new GenericDeclaration(type, new LinkedList<>());
	}
	
	public static @NotNull GenericDeclaration of(@NotNull Type type, @NotNull List<GenericDeclaration> nested) {
		return new GenericDeclaration(type, nested);
	}
	//endregion
	
	public @NotNull Type type() {
		return this.type;
	}
	
	@Unmodifiable
	public @NotNull List<GenericDeclaration> nested() {
		return List.copyOf(this.nested);
	}
}
