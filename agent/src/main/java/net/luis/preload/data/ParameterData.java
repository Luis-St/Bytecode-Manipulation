package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record ParameterData(String name, Type type, int index, List<TypeModifier> modifiers, List<AnnotationData> annotations) implements ASMData {
	
	@Override
	public String signature() {
		return null;
	}
}
