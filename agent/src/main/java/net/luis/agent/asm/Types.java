package net.luis.agent.asm;

import net.luis.agent.annotation.*;
import net.luis.agent.annotation.implementation.*;
import net.luis.agent.annotation.range.*;
import net.luis.agent.annotation.util.Local;
import net.luis.agent.annotation.util.This;
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

public class Types {
	
	public static final Type[] PRIMITIVES = { Type.BOOLEAN_TYPE, Type.CHAR_TYPE, Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE };
	public static final Type[] NUMBERS = { Type.BYTE_TYPE, Type.SHORT_TYPE, Type.INT_TYPE, Type.LONG_TYPE, Type.FLOAT_TYPE, Type.DOUBLE_TYPE };
	public static final Type[] WRAPPERS = {
		Type.getType(Boolean.class), Type.getType(Character.class), Type.getType(Byte.class), Type.getType(Short.class), Type.getType(Integer.class), Type.getType(Long.class), Type.getType(Float.class), Type.getType(Double.class)
	};
	
	public static final Type VOID = Type.VOID_TYPE;
	public static final Type BOOLEAN = Type.BOOLEAN_TYPE;
	public static final Type CHAR = Type.CHAR_TYPE;
	public static final Type BYTE = Type.BYTE_TYPE;
	public static final Type SHORT = Type.SHORT_TYPE;
	public static final Type INT = Type.INT_TYPE;
	public static final Type LONG = Type.LONG_TYPE;
	public static final Type FLOAT = Type.FLOAT_TYPE;
	public static final Type DOUBLE = Type.DOUBLE_TYPE;
	public static final Type STRING = Type.getType(String.class);
	public static final Type TYPE = Type.getType(Type.class);
	
	public static final Type INJECT_INTERFACE = Type.getType(InjectInterface.class);
	
	public static final Type IMPLEMENTED = Type.getType(Implemented.class);
	public static final Type ACCESSOR = Type.getType(Accessor.class);
	public static final Type ASSIGNOR = Type.getType(Assignor.class);
	public static final Type INVOKER = Type.getType(Invoker.class);
	public static final Type INJECTOR = Type.getType(Injector.class);
	public static final Type REDIRECTOR = Type.getType(Redirector.class);
	public static final Set<Type> IMPLEMENTATION_ANNOTATIONS = Set.of(IMPLEMENTED, ACCESSOR, ASSIGNOR, INVOKER, INJECTOR, REDIRECTOR);
	
	public static final Type THIS = Type.getType(This.class);
	public static final Type LOCAL = Type.getType(Local.class);
	
	public static final Type ABOVE = Type.getType(Above.class);
	public static final Type ABOVE_EQUAL = Type.getType(AboveEqual.class);
	public static final Type BELOW = Type.getType(Below.class);
	public static final Type BELOW_EQUAL = Type.getType(BelowEqual.class);
	
	public static final Type ASYNC = Type.getType(Async.class);
	public static final Type SCHEDULED = Type.getType(Scheduled.class);
	public static final Type CAUGHT = Type.getType(Caught.class);
	public static final Type DEFAULT = Type.getType(Default.class);
	public static final Type NOT_NULL = Type.getType(NotNull.class);
	public static final Type PATTERN = Type.getType(Pattern.class);
	public static final Type RESTRICTED_ACCESS = Type.getType(RestrictedAccess.class);
	
	public static final Type RUNTIME_UTILS = Type.getType("Lnet/luis/agent/generated/RuntimeUtils;");
	public static final Type DAEMON_THREAD_FACTORY = Type.getType("Lnet/luis/agent/generated/DaemonThreadFactory;");
	public static final Type COUNTING_RUNNABLE = Type.getType("Lnet/luis/agent/generated/CountingRunnable;");
	public static final Type CANCELABLE_RUNNABLE = Type.getType("Lnet/luis/agent/generated/CancelableRunnable;");
	public static final Type CONTEXT_RUNNABLE = Type.getType("Lnet/luis/agent/generated/ContextRunnable;");
	
	public static final Type RUNTIME_EXCEPTION = Type.getType("Ljava/lang/RuntimeException;");
	public static final Type ILLEGAL_ARGUMENT_EXCEPTION = Type.getType("Ljava/lang/IllegalArgumentException;");
	
	public static final Type MAP = Type.getType("Ljava/util/Map;");
	public static final Type CONCURRENT_HASH_MAP = Type.getType("Ljava/util/concurrent/ConcurrentHashMap;");
	
	public static final Type RUNNABLE = Type.getType("Ljava/lang/Runnable;");
	public static final Type CONSUMER = Type.getType("Ljava/util/function/Consumer;");
	public static final Type BI_CONSUMER = Type.getType("Ljava/util/function/BiConsumer;");
	public static final Type THREAD_FACTORY = Type.getType("Ljava/util/concurrent/ThreadFactory;");
	public static final Type SCHEDULED_FUTURE = Type.getType("Ljava/util/concurrent/ScheduledFuture;");
	
	public static final Type VOID_METHOD = Type.getType("()V");
	
	public static @NotNull Type convertToPrimitive(@NotNull Type wrapper) {
		int index = Utils.indexOf(WRAPPERS, wrapper);
		return index != -1 ? PRIMITIVES[index] : wrapper;
	}
	
	public static @NotNull Type convertToWrapper(@NotNull Type primitive) {
		int index = Utils.indexOf(PRIMITIVES, primitive);
		return index != -1 ? WRAPPERS[index] : primitive;
	}
	
	public static @NotNull String getSimpleName(@NotNull Type type) {
		String name = type.getClassName();
		int index = name.lastIndexOf('.');
		return index == -1 ? name : name.substring(index + 1);
	}
	
	public static boolean isSameType(@NotNull Type type, @NotNull String str) {
		boolean array = type.getSort() == Type.ARRAY;
		if (array) {
			String strElement = str;
			if (str.contains("[")) {
				strElement = str.substring(0, str.indexOf('['));
			}
			if (!isSameType(type.getElementType(), strElement)) {
				return false;
			}
			return type.getDimensions() == (str.length() - strElement.length()) / 2;
		} else if (str.contains("/")) {
			return type.getDescriptor().equalsIgnoreCase(str) || type.getInternalName().equalsIgnoreCase(str);
		} else if (str.contains(".")) {
			return type.getClassName().equals(str);
		} else if (Utils.indexOf(PRIMITIVES, type) != -1 && str.length() == 1) {
			return type.getDescriptor().equalsIgnoreCase(str);
		} else {
			return getSimpleName(type).equals(str);
		}
	}
}
