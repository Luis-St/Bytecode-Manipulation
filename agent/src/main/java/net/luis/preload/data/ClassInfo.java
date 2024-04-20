package net.luis.preload.data;

import net.luis.preload.ClassFileScanner;
import net.luis.preload.type.*;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ClassInfo(Type type, /*Nullable*/ String signature, TypeAccess access, ClassType classType, List<TypeModifier> modifiers, Type superType, List<Type> interfaces, List<AnnotationData> annotations) {

	public ClassContent getClassContent() {
		return ClassFileScanner.scanClassContent(this.type);
	}
}
