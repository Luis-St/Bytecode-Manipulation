package net.luis.agent.asm;

import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for working with ASM Tree API nodes (ClassNode, MethodNode, FieldNode)
 * and bridging them with the custom type system (TypeAccess, TypeModifier, etc.)
 *
 * @author Luis-St
 */
public class ASMTreeUtils {

	//region ClassNode utilities

	/**
	 * Get the Type of a ClassNode
	 */
	public static @NotNull Type getType(@NotNull ClassNode classNode) {
		return Type.getObjectType(classNode.name);
	}

	/**
	 * Get the simple name of a ClassNode
	 */
	public static @NotNull String getSimpleName(@NotNull ClassNode classNode) {
		int index = classNode.name.lastIndexOf('/');
		return index == -1 ? classNode.name : classNode.name.substring(index + 1);
	}

	/**
	 * Get TypeAccess from a ClassNode
	 */
	public static @NotNull TypeAccess getAccess(@NotNull ClassNode classNode) {
		return TypeAccess.fromAccess(classNode.access);
	}

	/**
	 * Get ClassType from a ClassNode
	 */
	public static @NotNull ClassType getClassType(@NotNull ClassNode classNode) {
		return ClassType.fromAccess(classNode.access);
	}

	/**
	 * Get TypeModifiers from a ClassNode
	 */
	public static @NotNull Set<TypeModifier> getModifiers(@NotNull ClassNode classNode) {
		return TypeModifier.fromClassAccess(classNode.access);
	}

	/**
	 * Get super type from a ClassNode
	 */
	public static @NotNull Type getSuperType(@NotNull ClassNode classNode) {
		return Type.getObjectType(classNode.superName);
	}

	/**
	 * Get interfaces from a ClassNode
	 */
	public static @NotNull List<Type> getInterfaces(@NotNull ClassNode classNode) {
		return classNode.interfaces.stream().map(Type::getObjectType).collect(Collectors.toList());
	}

	/**
	 * Get permitted subclasses from a ClassNode
	 */
	public static @NotNull List<Type> getPermittedSubclasses(@NotNull ClassNode classNode) {
		if (classNode.permittedSubclasses == null) {
			return Collections.emptyList();
		}
		return classNode.permittedSubclasses.stream().map(Type::getObjectType).collect(Collectors.toList());
	}

	/**
	 * Check if ClassNode is of a specific ClassType
	 */
	public static boolean is(@NotNull ClassNode classNode, @NotNull ClassType type) {
		return getClassType(classNode) == type;
	}

	/**
	 * Check if ClassNode has a specific modifier
	 */
	public static boolean is(@NotNull ClassNode classNode, @NotNull TypeModifier modifier) {
		return getModifiers(classNode).contains(modifier);
	}

	/**
	 * Find a method in a ClassNode by full signature
	 */
	public static @Nullable MethodNode getMethod(@NotNull ClassNode classNode, @NotNull String fullSignature) {
		for (MethodNode method : classNode.methods) {
			if ((method.name + method.desc).equals(fullSignature)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Find methods in a ClassNode by name only
	 */
	public static @NotNull List<MethodNode> getMethods(@NotNull ClassNode classNode, @NotNull String name) {
		return classNode.methods.stream()
			.filter(method -> method.name.equals(name))
			.collect(Collectors.toList());
	}

	/**
	 * Find a field in a ClassNode by name
	 */
	public static @Nullable FieldNode getField(@NotNull ClassNode classNode, @NotNull String name) {
		for (FieldNode field : classNode.fields) {
			if (field.name.equals(name)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Check if ClassNode has a specific annotation
	 */
	public static boolean hasAnnotation(@NotNull ClassNode classNode, @NotNull Type annotationType) {
		if (classNode.visibleAnnotations != null) {
			for (AnnotationNode annotation : classNode.visibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return true;
				}
			}
		}
		if (classNode.invisibleAnnotations != null) {
			for (AnnotationNode annotation : classNode.invisibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	//endregion

	//region MethodNode utilities

	/**
	 * Get the full signature (name + descriptor) of a MethodNode
	 */
	public static @NotNull String getFullSignature(@NotNull MethodNode methodNode) {
		return methodNode.name + methodNode.desc;
	}

	/**
	 * Get TypeAccess from a MethodNode
	 */
	public static @NotNull TypeAccess getAccess(@NotNull MethodNode methodNode) {
		return TypeAccess.fromAccess(methodNode.access);
	}

	/**
	 * Get MethodType from a MethodNode
	 */
	public static @NotNull MethodType getMethodType(@NotNull MethodNode methodNode) {
		return MethodType.fromName(methodNode.name);
	}

	/**
	 * Get TypeModifiers from a MethodNode
	 */
	public static @NotNull Set<TypeModifier> getModifiers(@NotNull MethodNode methodNode) {
		return TypeModifier.fromMethodAccess(methodNode.access);
	}

	/**
	 * Get the return type from a MethodNode
	 */
	public static @NotNull Type getReturnType(@NotNull MethodNode methodNode) {
		return Type.getReturnType(methodNode.desc);
	}

	/**
	 * Get parameter types from a MethodNode
	 */
	public static @NotNull Type[] getParameterTypes(@NotNull MethodNode methodNode) {
		return Type.getArgumentTypes(methodNode.desc);
	}

	/**
	 * Get exception types from a MethodNode
	 */
	public static @NotNull List<Type> getExceptions(@NotNull MethodNode methodNode) {
		if (methodNode.exceptions == null) {
			return Collections.emptyList();
		}
		return methodNode.exceptions.stream().map(Type::getObjectType).collect(Collectors.toList());
	}

	/**
	 * Check if MethodNode is of a specific MethodType
	 */
	public static boolean is(@NotNull MethodNode methodNode, @NotNull MethodType type) {
		MethodType methodType = getMethodType(methodNode);
		if (type == MethodType.CONSTRUCTOR && methodType == MethodType.PRIMARY_CONSTRUCTOR) {
			return true;
		}
		return methodType == type;
	}

	/**
	 * Check if MethodNode has a specific modifier
	 */
	public static boolean is(@NotNull MethodNode methodNode, @NotNull TypeModifier modifier) {
		return getModifiers(methodNode).contains(modifier);
	}

	/**
	 * Check if MethodNode has a specific access level
	 */
	public static boolean is(@NotNull MethodNode methodNode, @NotNull TypeAccess access) {
		return getAccess(methodNode) == access;
	}

	/**
	 * Check if MethodNode returns a specific type
	 */
	public static boolean returns(@NotNull MethodNode methodNode, @NotNull Type type) {
		return getReturnType(methodNode).equals(type);
	}

	/**
	 * Check if MethodNode returns any of the specified types
	 */
	public static boolean returnsAny(@NotNull MethodNode methodNode, Type @NotNull ... types) {
		Type returnType = getReturnType(methodNode);
		return Arrays.stream(types).anyMatch(returnType::equals);
	}

	/**
	 * Check if MethodNode is an implemented method (not abstract)
	 */
	public static boolean isImplementedMethod(@NotNull MethodNode methodNode) {
		return is(methodNode, MethodType.METHOD) && !is(methodNode, TypeModifier.ABSTRACT);
	}

	/**
	 * Check if method has a specific annotation
	 */
	public static boolean hasAnnotation(@NotNull MethodNode methodNode, @NotNull Type annotationType) {
		if (methodNode.visibleAnnotations != null) {
			for (AnnotationNode annotation : methodNode.visibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return true;
				}
			}
		}
		if (methodNode.invisibleAnnotations != null) {
			for (AnnotationNode annotation : methodNode.invisibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get the number of parameters in a MethodNode
	 */
	public static int getParameterCount(@NotNull MethodNode methodNode) {
		return getParameterTypes(methodNode).length;
	}

	/**
	 * Get the number of exceptions in a MethodNode
	 */
	public static int getExceptionCount(@NotNull MethodNode methodNode) {
		return methodNode.exceptions == null ? 0 : methodNode.exceptions.size();
	}

	/**
	 * Check if a parameter has a specific annotation
	 */
	public static boolean hasParameterAnnotation(@NotNull MethodNode methodNode, int parameterIndex, @NotNull Type annotationType) {
		return getParameterAnnotation(methodNode, parameterIndex, annotationType) != null;
	}

	/**
	 * Get a parameter annotation
	 */
	public static @Nullable AnnotationNode getParameterAnnotation(@NotNull MethodNode methodNode, int parameterIndex, @NotNull Type annotationType) {
		if (methodNode.visibleParameterAnnotations != null && parameterIndex < methodNode.visibleParameterAnnotations.length) {
			List<AnnotationNode> annotations = methodNode.visibleParameterAnnotations[parameterIndex];
			if (annotations != null) {
				for (AnnotationNode annotation : annotations) {
					if (Type.getType(annotation.desc).equals(annotationType)) {
						return annotation;
					}
				}
			}
		}
		if (methodNode.invisibleParameterAnnotations != null && parameterIndex < methodNode.invisibleParameterAnnotations.length) {
			List<AnnotationNode> annotations = methodNode.invisibleParameterAnnotations[parameterIndex];
			if (annotations != null) {
				for (AnnotationNode annotation : annotations) {
					if (Type.getType(annotation.desc).equals(annotationType)) {
						return annotation;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Check if any parameter has a specific annotation
	 */
	public static boolean hasAnyParameterWithAnnotation(@NotNull MethodNode methodNode, @NotNull Type annotationType) {
		if (methodNode.visibleParameterAnnotations != null) {
			for (List<AnnotationNode> annotations : methodNode.visibleParameterAnnotations) {
				if (annotations != null) {
					for (AnnotationNode annotation : annotations) {
						if (Type.getType(annotation.desc).equals(annotationType)) {
							return true;
						}
					}
				}
			}
		}
		if (methodNode.invisibleParameterAnnotations != null) {
			for (List<AnnotationNode> annotations : methodNode.invisibleParameterAnnotations) {
				if (annotations != null) {
					for (AnnotationNode annotation : annotations) {
						if (Type.getType(annotation.desc).equals(annotationType)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}


	/**
	 * Get the load index for a parameter (accounting for 'this' in non-static methods)
	 */
	public static int getParameterLoadIndex(@NotNull MethodNode methodNode, int parameterIndex) {
		boolean isStatic = is(methodNode, TypeModifier.STATIC);
		if (isStatic) {
			Type[] paramTypes = getParameterTypes(methodNode);
			int loadIndex = 0;
			for (int i = 0; i < parameterIndex; i++) {
				loadIndex += paramTypes[i].getSize();
			}
			return loadIndex;
		} else {
			Type[] paramTypes = getParameterTypes(methodNode);
			int loadIndex = 1; // 'this' is at index 0
			for (int i = 0; i < parameterIndex; i++) {
				loadIndex += paramTypes[i].getSize();
			}
			return loadIndex;
		}
	}

	/**
	 * Check if a variable index is a local variable (not a parameter)
	 */
	public static boolean isLocalVariable(@NotNull MethodNode methodNode, int varIndex) {
		Type[] paramTypes = getParameterTypes(methodNode);
		boolean isStatic = is(methodNode, TypeModifier.STATIC);
		int paramSlots = isStatic ? 0 : 1; // 'this' takes slot 0 in non-static methods
		for (Type paramType : paramTypes) {
			paramSlots += paramType.getSize();
		}
		return varIndex >= paramSlots;
	}

	//endregion

	//region FieldNode utilities

	/**
	 * Get TypeAccess from a FieldNode
	 */
	public static @NotNull TypeAccess getAccess(@NotNull FieldNode fieldNode) {
		return TypeAccess.fromAccess(fieldNode.access);
	}

	/**
	 * Get TypeModifiers from a FieldNode
	 */
	public static @NotNull Set<TypeModifier> getModifiers(@NotNull FieldNode fieldNode) {
		return TypeModifier.fromFieldAccess(fieldNode.access);
	}

	/**
	 * Get the type from a FieldNode
	 */
	public static @NotNull Type getType(@NotNull FieldNode fieldNode) {
		return Type.getType(fieldNode.desc);
	}

	/**
	 * Check if FieldNode has a specific modifier
	 */
	public static boolean is(@NotNull FieldNode fieldNode, @NotNull TypeModifier modifier) {
		return getModifiers(fieldNode).contains(modifier);
	}

	/**
	 * Check if FieldNode has a specific access level
	 */
	public static boolean is(@NotNull FieldNode fieldNode, @NotNull TypeAccess access) {
		return getAccess(fieldNode) == access;
	}

	/**
	 * Check if field has a specific annotation
	 */
	public static boolean hasAnnotation(@NotNull FieldNode fieldNode, @NotNull Type annotationType) {
		if (fieldNode.visibleAnnotations != null) {
			for (AnnotationNode annotation : fieldNode.visibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return true;
				}
			}
		}
		if (fieldNode.invisibleAnnotations != null) {
			for (AnnotationNode annotation : fieldNode.invisibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return true;
				}
			}
		}
		return false;
	}

	//endregion

	//region AnnotationNode utilities

	/**
	 * Find an annotation on a ClassNode
	 */
	public static @Nullable AnnotationNode getAnnotation(@NotNull ClassNode classNode, @NotNull Type annotationType) {
		if (classNode.visibleAnnotations != null) {
			for (AnnotationNode annotation : classNode.visibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return annotation;
				}
			}
		}
		if (classNode.invisibleAnnotations != null) {
			for (AnnotationNode annotation : classNode.invisibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Find an annotation on a MethodNode
	 */
	public static @Nullable AnnotationNode getAnnotation(@NotNull MethodNode methodNode, @NotNull Type annotationType) {
		if (methodNode.visibleAnnotations != null) {
			for (AnnotationNode annotation : methodNode.visibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return annotation;
				}
			}
		}
		if (methodNode.invisibleAnnotations != null) {
			for (AnnotationNode annotation : methodNode.invisibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Find an annotation on a FieldNode
	 */
	public static @Nullable AnnotationNode getAnnotation(@NotNull FieldNode fieldNode, @NotNull Type annotationType) {
		if (fieldNode.visibleAnnotations != null) {
			for (AnnotationNode annotation : fieldNode.visibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return annotation;
				}
			}
		}
		if (fieldNode.invisibleAnnotations != null) {
			for (AnnotationNode annotation : fieldNode.invisibleAnnotations) {
				if (Type.getType(annotation.desc).equals(annotationType)) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Get an annotation value by key
	 */
	public static @Nullable Object getAnnotationValue(@NotNull AnnotationNode annotation, @NotNull String key) {
		if (annotation.values == null) {
			return null;
		}
		for (int i = 0; i < annotation.values.size(); i += 2) {
			if (annotation.values.get(i).equals(key)) {
				return annotation.values.get(i + 1);
			}
		}
		return null;
	}

	/**
	 * Get an annotation value by key with a default value
	 */
	@SuppressWarnings("unchecked")
	public static <T> @NotNull T getAnnotationValue(@NotNull AnnotationNode annotation, @NotNull String key, @NotNull T defaultValue) {
		Object value = getAnnotationValue(annotation, key);
		return value != null ? (T) value : defaultValue;
	}

	/**
	 * Get all annotations (visible and invisible) from a MethodNode
	 */
	public static @NotNull List<AnnotationNode> getAllAnnotations(@NotNull MethodNode methodNode) {
		List<AnnotationNode> annotations = new ArrayList<>();
		if (methodNode.visibleAnnotations != null) {
			annotations.addAll(methodNode.visibleAnnotations);
		}
		if (methodNode.invisibleAnnotations != null) {
			annotations.addAll(methodNode.invisibleAnnotations);
		}
		return annotations;
	}

	/**
	 * Get all annotations (visible and invisible) from a ClassNode
	 */
	public static @NotNull List<AnnotationNode> getAllAnnotations(@NotNull ClassNode classNode) {
		List<AnnotationNode> annotations = new ArrayList<>();
		if (classNode.visibleAnnotations != null) {
			annotations.addAll(classNode.visibleAnnotations);
		}
		if (classNode.invisibleAnnotations != null) {
			annotations.addAll(classNode.invisibleAnnotations);
		}
		return annotations;
	}

	/**
	 * Get all annotations (visible and invisible) from a FieldNode
	 */
	public static @NotNull List<AnnotationNode> getAllAnnotations(@NotNull FieldNode fieldNode) {
		List<AnnotationNode> annotations = new ArrayList<>();
		if (fieldNode.visibleAnnotations != null) {
			annotations.addAll(fieldNode.visibleAnnotations);
		}
		if (fieldNode.invisibleAnnotations != null) {
			annotations.addAll(fieldNode.invisibleAnnotations);
		}
		return annotations;
	}

	//endregion

	//region Signature utilities

	/**
	 * Get a debug signature for a MethodNode (for error reporting)
	 */
	public static @NotNull String getDebugSignature(@NotNull Type owner, @NotNull MethodNode methodNode) {
		String params = Arrays.stream(getParameterTypes(methodNode))
			.map(Types::getSimpleName)
			.collect(Collectors.joining(", ", "(", ")"));
		return owner.getClassName() + "#" + methodNode.name + params;
	}

	/**
	 * Get a debug signature for a FieldNode (for error reporting)
	 */
	public static @NotNull String getDebugSignature(@NotNull Type owner, @NotNull FieldNode fieldNode) {
		return owner.getClassName() + "#" + fieldNode.name + " : " + getType(fieldNode).getClassName();
	}

	/**
	 * Get a source signature for a MethodNode (simple owner + method name)
	 */
	public static @NotNull String getSourceSignature(@NotNull Type owner, @NotNull MethodNode methodNode) {
		return Types.getSimpleName(owner) + "#" + methodNode.name;
	}

	/**
	 * Get a source signature for a FieldNode (simple owner + field name)
	 */
	public static @NotNull String getSourceSignature(@NotNull Type owner, @NotNull FieldNode fieldNode) {
		return Types.getSimpleName(owner) + "#" + fieldNode.name;
	}

	//endregion
}
