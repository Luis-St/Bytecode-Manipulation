package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public record RecordComponentData(String name, Type type, String signature, List<AnnotationData> annotations) implements ASMData {
	
	@Override
	public List<TypeModifier> modifiers() {
		return List.of();
	}
}
