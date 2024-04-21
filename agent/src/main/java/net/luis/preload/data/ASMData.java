package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public interface ASMData {
	
	String name();
	
	Type type();
	
	String signature();
	
	List<TypeModifier> modifiers();
	
	List<AnnotationData> annotations();
}
