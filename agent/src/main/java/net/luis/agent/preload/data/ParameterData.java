package net.luis.agent.preload.data;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Luis-St
 *
 */

public record ParameterData(@NotNull String name, @NotNull Type type, int index, @NotNull Set<TypeModifier> modifiers, @NotNull Map<Type, AnnotationData> annotations) implements ASMData {
	
	@Override
	public @NotNull TypeAccess access() {
		return TypeAccess.PUBLIC;
	}
	
	@Override
	public @Nullable String signature() {
		return null;
	}
	
	public boolean isNamed() {
		return !this.name.equals("arg" + this.index);
	}
	
	public @NotNull String getMessageName() {
		if (this.isNamed()) {
			return Utils.capitalize(Utils.getSeparated(this.name));
		}
		return Utils.capitalize(Utils.getSeparated(ASMUtils.getSimpleName(this.type()))) + " (parameter #" + this.index() + ")";
	}
}
