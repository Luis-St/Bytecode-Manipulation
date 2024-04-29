package net.luis.preload.data;

import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Luis-St
 *
 */

public record FieldData(String name, Type type, String signature, TypeAccess access, Set<TypeModifier> modifiers, Map<Type, AnnotationData> annotations, /*Nullable*/ Object initialValue) implements ASMData {
	
	public String getFieldSignature() {
		return this.type + this.name;
	}
}
