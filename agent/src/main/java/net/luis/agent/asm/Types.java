package net.luis.agent.asm;

import net.luis.agent.annotation.*;
import net.luis.agent.annotation.unused.Above;
import net.luis.agent.annotation.unused.Below;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.objectweb.asm.Type;

import java.util.Set;

/**
 *
 * @author Luis-St
 *
 */

public interface Types {
	
	Type[] PRIMITIVES = { Type.VOID_TYPE, Type.BOOLEAN_TYPE, Type.CHAR_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE };
	Type[] NUMBERS = { Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE };
	
	Type STRING = Type.getType(String.class);
	
	Type GENERATED = Type.getType(Generated.class);
	
	Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	Type IMPLEMENTED = Type.getType(Implemented.class);
	Type ACCESSOR = Type.getType(Accessor.class);
	Type ASSIGNOR = Type.getType(Assignor.class);
	Type INVOKER = Type.getType(Invoker.class);
	Set<Type> IMPLEMENTATION_ANNOTATIONS = Set.of(IMPLEMENTED, ACCESSOR, ASSIGNOR, INVOKER);
	
	Type DEFAULT = Type.getType(Default.class);
	Type NOT_NULL = Type.getType(NotNull.class);
	Type PATTERN = Type.getType(Pattern.class);
	
	Type RANGE = Type.getType(Range.class);
	Type ABOVE = Type.getType(Above.class);
	Type BELOW = Type.getType(Below.class);
}
