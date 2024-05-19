package net.luis.agent.asm;

import net.luis.agent.annotation.*;
import net.luis.agent.annotation.implementation.*;
import net.luis.agent.annotation.range.*;
import net.luis.agent.annotation.Async;
import net.luis.agent.annotation.Caught;
import net.luis.agent.annotation.Scheduled;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;
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
	
	Type VOID = Type.VOID_TYPE;
	Type STRING = Type.getType(String.class);
	Type TYPE = Type.getType(Type.class);
	
	Type VOID_METHOD = Type.getType("()V");
	
	Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	Type IMPLEMENTED = Type.getType(Implemented.class);
	Type ACCESSOR = Type.getType(Accessor.class);
	Type ASSIGNOR = Type.getType(Assignor.class);
	Type INVOKER = Type.getType(Invoker.class);
	Set<Type> IMPLEMENTATION_ANNOTATIONS = Set.of(IMPLEMENTED, ACCESSOR, ASSIGNOR, INVOKER);
	
	Type DEFAULT = Type.getType(Default.class);
	Type NOT_NULL = Type.getType(NotNull.class);
	Type PATTERN = Type.getType(Pattern.class);
	
	Type ABOVE = Type.getType(Above.class);
	Type ABOVE_EQUAL = Type.getType(AboveEqual.class);
	Type BELOW = Type.getType(Below.class);
	Type BELOW_EQUAL = Type.getType(BelowEqual.class);
	
	Type ASYNC = Type.getType(Async.class);
	Type SCHEDULED = Type.getType(Scheduled.class);
	Type CAUGHT = Type.getType(Caught.class);
}
