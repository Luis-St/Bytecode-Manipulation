package net.luis.agent.asm;

import net.luis.agent.AgentContext;
import net.luis.agent.preload.data.Class;
import net.luis.agent.preload.data.Method;
import net.luis.agent.preload.data.Parameter;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Stream;

/**
 @author Luis-St */

public class ASMUtils {
	
	public static void saveClass(@NotNull File file, byte @NotNull [] data) {
		try {
			Files.deleteIfExists(file.toPath());
			Files.createDirectories(file.getParentFile().toPath());
			Files.write(file.toPath(), data);
		} catch (Exception e) {
			System.err.println("Failed to save class file: " + file.getName());
		}
	}
	
	public static @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> createTargetsLookup(@NotNull Type annotationType) {
		Map<String, List<String>> lookup = new HashMap<>();
		AgentContext.get().stream().filter(data -> data.isAnnotatedWith(annotationType)).forEach(data -> {
			List<Type> types = data.getAnnotation(annotationType).get("targets");
			if (types == null || types.isEmpty()) {
				return;
			}
			for (Type target : types) {
				lookup.computeIfAbsent(target.getInternalName(), k -> new ArrayList<>()).add(data.getType().getInternalName());
			}
		});
		return lookup;
	}
	
	public static @NotNull String getReturnTypeSignature(@NotNull Method method) {
		String signature = method.getGenericSignature();
		if (signature == null || signature.isEmpty()) {
			return "";
		}
		int index = signature.indexOf(')');
		return signature.substring(index + 1);
	}
	
	public static @NotNull String getParameterTypesSignature(@NotNull Method method) {
		String signature = method.getGenericSignature();
		if (signature == null || signature.isEmpty()) {
			return "";
		}
		int start = signature.indexOf('(');
		int end = signature.indexOf(')');
		return signature.substring(start + 1, end);
	}
	
	public static @NotNull String getSimpleName(@NotNull Type type) {
		String name = type.getClassName();
		int index = name.lastIndexOf('.');
		return index == -1 ? name : name.substring(index + 1);
	}
	
	public static boolean isPrimitive(@NotNull Type type) {
		return type.getSort() >= Type.BOOLEAN && type.getSort() <= Type.DOUBLE;
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
		} else if (isPrimitive(type) && str.length() == 1) {
			return type.getDescriptor().equalsIgnoreCase(str);
		} else {
			return getSimpleName(type).equals(str);
		}
	}
	
	public static @NotNull List<Method> getBySignature(@NotNull String signature, @NotNull Class data) {
		List<Method> methods = new ArrayList<>();
		boolean specific = signature.contains("(") && signature.contains(")");
		boolean notFull = !specific || signature.endsWith(")");
		
		String name = specific ? signature.substring(0, signature.indexOf('(')).strip() : signature;
		List<String> parameters = specific ? Stream.of(signature.substring(signature.indexOf('(') + 1, signature.indexOf(')')).split(",")).map(String::strip).toList() : new ArrayList<>();
		List<Method> possibleMethods = name.isEmpty() ? new ArrayList<>(data.getMethods().values()) : data.getMethods(name);
		if (parameters.isEmpty() && notFull) {
			return possibleMethods;
		}
		for (Method method : possibleMethods) {
			if (method.getFullSignature().equals(signature)) {
				methods.add(method);
				break;
			}
			boolean possible = true;
			for (Parameter parameter : method.getParameters().values()) {
				Type type = parameter.getType();
				int index = parameter.getIndex();
				if (index >= parameters.size()) {
					possible = false;
					break;
				}
				String str = parameters.get(index);
				possible = isSameType(type, str);
			}
			if (possible && method.getParameterCount() == parameters.size()) {
				methods.add(method);
			}
		}
		return methods;
	}
	
	public static boolean matchesTarget(@NotNull String target, @NotNull Type owner, @NotNull String name, @Nullable Type descriptor) {
		boolean specifiesOwner = target.contains("#") && !target.startsWith("#");
		boolean specifiesParameters = target.contains("(") && target.contains(")");
		
		String targetOwner = specifiesOwner ? target.substring(0, target.indexOf('#')).strip() : "";
		if (!targetOwner.isEmpty() && !isSameType(owner, targetOwner)) {
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
					if (!isSameType(methodParameters[i], targetParameters.get(i))) {
						return false;
					}
				}
			}
		}
		
		boolean specifiesReturnType = specifiesParameters && !target.endsWith(")");
		String targetReturnType = specifiesReturnType ? target.substring(target.indexOf(')') + 1).strip() : "";
		return targetReturnType.isEmpty() || isSameType(descriptor.getReturnType(), targetReturnType);
	}
	
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
}
