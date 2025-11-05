package net.luis.agent.asm;

import net.luis.agent.Agent;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

import static net.luis.agent.asm.Types.*;

/**
 @author Luis-St */

public class ASMUtils {

	private static final java.lang.reflect.Field LABEL_LINE;
	private static final java.lang.reflect.Field LABEL_FLAGS;
	private static final int FLAG_LINE_NUMBER = 128;

	public static void saveClass(@NotNull File file, byte @NotNull [] data) {
		try {
			Files.deleteIfExists(file.toPath());
			Files.createDirectories(file.getParentFile().toPath());
			Files.write(file.toPath(), data);
		} catch (Exception e) {
			System.err.println("Failed to save class file: " + file.getName());
		}
	}

	public static int getLine(@NotNull Label label) {
		int line = -1;
		try {
			short flags = LABEL_FLAGS.getShort(label);
			if ((flags & FLAG_LINE_NUMBER) != 0) {
				line = LABEL_LINE.getInt(label);
			}
		} catch (Exception ignored) {}
		return line;
	}

	public static @NotNull List<MethodNode> getBySignature(@NotNull String signature, @NotNull ClassNode data) {
		List<MethodNode> methods = new ArrayList<>();
		boolean specific = signature.contains("(") && signature.contains(")");
		boolean notFull = !specific || signature.endsWith(")");

		String name = specific ? signature.substring(0, signature.indexOf('(')).strip() : signature;
		List<String> parameters = specific ? Stream.of(signature.substring(signature.indexOf('(') + 1, signature.indexOf(')')).split(",")).map(String::strip).toList() : new ArrayList<>();
		List<MethodNode> possibleMethods = name.isEmpty() ? new ArrayList<>(data.methods) : ASMTreeUtils.getMethods(data, name);
		if (parameters.isEmpty() && notFull) {
			return possibleMethods;
		}
		for (MethodNode method : possibleMethods) {
			String fullSignature = ASMTreeUtils.getFullSignature(method);
			if (fullSignature.equals(signature)) {
				methods.add(method);
				break;
			}
			boolean possible = true;
			Type[] parameterTypes = ASMTreeUtils.getParameterTypes(method);
			if (parameterTypes.length != parameters.size()) {
				continue;
			}
			for (int i = 0; i < parameterTypes.length; i++) {
				Type type = parameterTypes[i];
				String str = parameters.get(i);
				if (!Types.isSameType(type, str)) {
					possible = false;
					break;
				}
			}
			if (possible) {
				methods.add(method);
			}
		}
		return methods;
	}
	
	public static boolean matchesTarget(@NotNull String target, @NotNull Type owner, @NotNull String name, @Nullable Type descriptor) {
		boolean specifiesOwner = target.contains("#") && !target.startsWith("#");
		boolean specifiesParameters = target.contains("(") && target.contains(")");
		
		String targetOwner = specifiesOwner ? target.substring(0, target.indexOf('#')).strip() : "";
		if (!targetOwner.isEmpty() && !Types.isSameType(owner, targetOwner)) {
			return false;
		}
		
		String targetMethod = getTargetMethod(target, specifiesOwner, specifiesParameters).strip();
		if (!name.equals(targetMethod)) {
			return false;
		}
		
		if (specifiesParameters) {
			if (descriptor == null) {
				return false;
			}
			String parameters = Utils.deleteWhitespace(target.substring(target.indexOf('(') + 1, target.indexOf(')')));
			Type targetDescriptor = tryParseMethodType("(" + parameters + ")");
			if (targetDescriptor != null && !Arrays.equals(descriptor.getArgumentTypes(), targetDescriptor.getArgumentTypes())) {
				return false;
			} else if (targetDescriptor == null) {
				List<String> targetParameters = Stream.of(parameters.split(",")).map(String::strip).toList();
				Type[] methodParameters = descriptor.getArgumentTypes();
				if (targetParameters.size() != methodParameters.length) {
					return false;
				}
				for (int i = 0; i < methodParameters.length; i++) {
					if (!Types.isSameType(methodParameters[i], targetParameters.get(i))) {
						return false;
					}
				}
			}
		}
		
		boolean specifiesReturnType = specifiesParameters && !target.endsWith(")");
		String targetReturnType = specifiesReturnType ? target.substring(target.indexOf(')') + 1).strip() : "";
		return targetReturnType.isEmpty() || Types.isSameType(descriptor.getReturnType(), targetReturnType);
	}
	
	//region Helper methods
	private static @Nullable Type tryParseMethodType(@NotNull String type) {
		try {
			Type methodType = Type.getMethodType(type);
			methodType.getArgumentTypes();
			return methodType;
		} catch (Exception e) {
			return null;
		}
	}
	
	private static @NotNull String getTargetMethod(@NotNull String target, boolean specifiesOwner, boolean specifiesParameters) {
		if (specifiesOwner && specifiesParameters) {
			return target.substring(target.indexOf('#') + 1, target.indexOf('('));
		} else if (specifiesOwner) {
			return target.substring(target.indexOf('#') + 1);
		} else if (specifiesParameters) {
			return target.substring(0, target.indexOf('('));
		}
		return target;
	}
	//endregion
	
	static {
		try {
			LABEL_LINE = Label.class.getDeclaredField("lineNumber");
			LABEL_LINE.setAccessible(true);
			LABEL_FLAGS = Label.class.getDeclaredField("flags");
			LABEL_FLAGS.setAccessible(true);
		} catch (NoSuchFieldException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
}
