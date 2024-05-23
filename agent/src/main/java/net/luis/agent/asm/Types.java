package net.luis.agent.asm;

import net.luis.agent.annotation.*;
import net.luis.agent.annotation.implementation.*;
import net.luis.agent.annotation.range.*;
import net.luis.agent.util.Utils;
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
	
	Type[] PRIMITIVES = { Type.BOOLEAN_TYPE, Type.CHAR_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE };
	Type[] NUMBERS = { Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE };
	Type[] WRAPPERS = {
		Type.getType(Boolean.class), Type.getType(Character.class), Type.getType(Byte.class), Type.getType(Short.class), Type.getType(Integer.class), Type.getType(Long.class), Type.getType(Float.class), Type.getType(Double.class)
	};
	
	Type VOID = Type.VOID_TYPE;
	Type BOOLEAN = Type.BOOLEAN_TYPE;
	Type CHAR = Type.CHAR_TYPE;
	Type BYTE = Type.BYTE_TYPE;
	Type SHORT = Type.SHORT_TYPE;
	Type INT = Type.INT_TYPE;
	Type LONG = Type.LONG_TYPE;
	Type FLOAT = Type.FLOAT_TYPE;
	Type DOUBLE = Type.DOUBLE_TYPE;
	Type STRING = Type.getType(String.class);
	Type TYPE = Type.getType(Type.class);
	
	Type VOID_METHOD = Type.getType("()V");
	
	Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	Type IMPLEMENTED = Type.getType(Implemented.class);
	Type ACCESSOR = Type.getType(Accessor.class);
	Type ASSIGNOR = Type.getType(Assignor.class);
	Type INVOKER = Type.getType(Invoker.class);
	Type INJECTOR = Type.getType(Injector.class);
	Set<Type> IMPLEMENTATION_ANNOTATIONS = Set.of(IMPLEMENTED, ACCESSOR, ASSIGNOR, INVOKER, INJECTOR);
	
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
	
	static @NotNull Type convertToPrimitive(@NotNull Type wrapper) {
		int index = Utils.indexOf(WRAPPERS, wrapper);
		return index != -1 ? PRIMITIVES[index] : wrapper;
	}
	
	static @NotNull Type convertToWrapper(@NotNull Type primitive) {
		int index = Utils.indexOf(PRIMITIVES, primitive);
		return index != -1 ? WRAPPERS[index] : primitive;
	}
}
