package net.luis.preload;

import net.luis.preload.data.ClassContent;
import net.luis.preload.data.ClassInfo;
import net.luis.preload.type.*;
import org.objectweb.asm.Type;

import java.util.function.BiPredicate;

/**
 *
 * @author Luis-St
 *
 */

public interface ClassDataPredicate extends BiPredicate<ClassInfo, ClassContent> {
	
	static ClassDataPredicate ofType(ClassType type) {
		return (info, content) -> info.classType() == type;
	}
	
	static ClassDataPredicate ofAccess(TypeAccess access) {
		return (info, content) -> info.access() == access;
	}
	
	static ClassDataPredicate withModifier(TypeModifier modifier) {
		return (info, content) -> info.modifiers().contains(modifier);
	}
	
	static ClassDataPredicate extendsClass(Type type) {
		return (info, content) -> info.superType() != null && info.superType().equals(type);
	}
	
	static ClassDataPredicate implementsInterface(Type type) {
		return (info, content) -> info.interfaces().contains(type);
	}
	
	static ClassDataPredicate annotatedWith(Type type) {
		return (info, content) -> info.annotations().stream().anyMatch(annotation -> annotation.type().equals(type));
	}
	
	@Override
	boolean test(ClassInfo info, ClassContent content);
}
