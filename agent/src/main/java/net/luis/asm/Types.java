package net.luis.asm;

import net.luis.annotation.*;
import org.objectweb.asm.Type;

import java.util.Set;

/**
 *
 * @author Luis-St
 *
 */

public interface Types {
	
	Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	Type IMPLEMENTED = Type.getType(Implemented.class);
	Type ACCESSOR = Type.getType(Accessor.class);
	Type ASSIGNOR = Type.getType(Assignor.class);
	Type INVOKER = Type.getType(Invoker.class);
	
	Type GENERATED = Type.getType(Generated.class);
	
	Set<Type> IMPLEMENTATION_ANNOTATIONS = Set.of(IMPLEMENTED, ACCESSOR, ASSIGNOR, INVOKER);
}
