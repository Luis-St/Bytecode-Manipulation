package net.luis.preload;

import net.luis.preload.data.ClassContent;
import net.luis.preload.data.ClassInfo;
import net.luis.preload.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.function.BiPredicate;

/**
 *
 * @author Luis-St
 *
 */

public interface ClassDataPredicate extends BiPredicate<ClassInfo, ClassContent> {
	
	static @NotNull ClassDataPredicate ofType(@Nullable ClassType type) {
		return (info, content) -> info.classType() == type;
	}
	
	static @NotNull ClassDataPredicate ofAccess(@Nullable TypeAccess access) {
		return (info, content) -> info.access() == access;
	}
	
	static @NotNull ClassDataPredicate withModifier(@Nullable TypeModifier modifier) {
		return (info, content) -> info.modifiers().contains(modifier);
	}
	
	static @NotNull ClassDataPredicate extendsClass(@Nullable Type type) {
		return (info, content) -> info.superType() != null && info.superType().equals(type);
	}
	
	static @NotNull ClassDataPredicate implementsInterface(@Nullable Type type) {
		return (info, content) -> info.interfaces().contains(type);
	}
	
	static @NotNull ClassDataPredicate annotatedWith(@Nullable Type type) {
		return (info, content) -> info.isAnnotatedWith(type);
	}
	
	@Override
	boolean test(@NotNull ClassInfo info, @NotNull ClassContent content);
}
