package net.luis.agent.asm;

import net.luis.agent.AgentContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.ClassType;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.List;
import java.util.Map;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class Instrumentations {
	
	public static final String METAFACTORY_DESCRIPTOR = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
	public static final Handle METAFACTORY_HANDLE = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", METAFACTORY_DESCRIPTOR, false);
	
	//region Opcodes
	public static boolean isLoad(int opcode) {
		return Opcodes.ALOAD >= opcode && opcode >= Opcodes.ILOAD;
	}
	
	public static boolean isArrayLoad(int opcode) {
		return Opcodes.SALOAD >= opcode && opcode >= Opcodes.IALOAD;
	}
	
	public static boolean isStore(int opcode) {
		return Opcodes.ASTORE >= opcode && opcode >= Opcodes.ISTORE;
	}
	
	public static boolean isArrayStore(int opcode) {
		return Opcodes.SASTORE >= opcode && opcode >= Opcodes.IASTORE;
	}
	
	public static boolean isConst(int opcode) {
		return Opcodes.DCONST_1 >= opcode && opcode >= Opcodes.ACONST_NULL;
	}
	
	public static boolean isReturn(int opcode) {
		return Opcodes.RETURN >= opcode && opcode >= Opcodes.IRETURN;
	}
	
	public static boolean isNumericOperand(int opcode) {
		return Opcodes.LXOR >= opcode && opcode >= Opcodes.IADD;
	}
	
	public static boolean isNumericOperand(int opcode, int lastOpcode, @NotNull String value) {
		if (Opcodes.DADD >= opcode && opcode >= Opcodes.IADD) {
			return "+".equals(value);
		} else if (Opcodes.DSUB >= opcode && opcode >= Opcodes.ISUB) {
			return "-".equals(value);
		} else if (Opcodes.DMUL >= opcode && opcode >= Opcodes.IMUL) {
			return "*".equals(value);
		} else if (Opcodes.DDIV >= opcode && opcode >= Opcodes.IDIV) {
			return "/".equals(value);
		} else if (Opcodes.DREM >= opcode && opcode >= Opcodes.IREM) {
			return "%".equals(value);
		} else if (Opcodes.DNEG >= opcode && opcode >= Opcodes.INEG) {
			return "neg".equals(value);
		} else if (Opcodes.LAND >= opcode && opcode >= Opcodes.IAND) {
			return "&".equals(value);
		} else if (Opcodes.LOR >= opcode && opcode >= Opcodes.IOR) {
			return "|".equals(value);
		} else if (Opcodes.LXOR >= opcode && opcode >= Opcodes.IXOR) {
			if (lastOpcode == Opcodes.ICONST_M1) {
				return "~".equals(value);
			}
			return "^".equals(value);
		} else if (Opcodes.LSHL >= opcode && opcode >= Opcodes.ISHL) {
			return "<<".equals(value);
		} else if (Opcodes.LSHR >= opcode && opcode >= Opcodes.ISHR) {
			return ">>".equals(value);
		} else if (Opcodes.LUSHR >= opcode && opcode >= Opcodes.IUSHR) {
			return ">>>".equals(value);
		}
		return false;
	}
	
	public static boolean isConstant(int opcode, @NotNull String value) {
		if (opcode == Opcodes.ACONST_NULL) {
			return "null".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.ICONST_M1) {
			return "-1".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.ICONST_0) {
			return "0".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.ICONST_1) {
			return "1".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
		} else if (Opcodes.ICONST_5 >= opcode && opcode >= Opcodes.ICONST_2) {
			return value.equalsIgnoreCase(String.valueOf(opcode - Opcodes.ICONST_0));
		} else if (opcode == Opcodes.LCONST_0) {
			return "0".equalsIgnoreCase(value) || "0L".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.LCONST_1) {
			return "1".equalsIgnoreCase(value) || "1L".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.FCONST_0) {
			return "0.0".equalsIgnoreCase(value) || "0.0F".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.FCONST_1) {
			return "1.0".equalsIgnoreCase(value) || "1.0F".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.FCONST_2) {
			return "2.0".equalsIgnoreCase(value) || "2.0F".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.DCONST_0) {
			return "0.0".equalsIgnoreCase(value) || "0.0D".equalsIgnoreCase(value);
		} else if (opcode == Opcodes.DCONST_1) {
			return "1.0".equalsIgnoreCase(value) || "1.0D".equalsIgnoreCase(value);
		}
		return false;
	}
	
	public static boolean isCompare(@NotNull String compare, int opcode) {
		return switch (compare) {
			case "==" -> opcode == Opcodes.IF_ICMPNE || opcode == Opcodes.IFNE || opcode == Opcodes.IF_ACMPNE || opcode == Opcodes.IFNONNULL;
			case "!=" -> opcode == Opcodes.IF_ICMPEQ || opcode == Opcodes.IFEQ || opcode == Opcodes.IF_ACMPEQ || opcode == Opcodes.IFNULL;
			case "<" -> opcode == Opcodes.IF_ICMPGE || opcode == Opcodes.IFGE;
			case "<=" -> opcode == Opcodes.IF_ICMPGT || opcode == Opcodes.IFGT;
			case ">" -> opcode == Opcodes.IF_ICMPLE || opcode == Opcodes.IFLE;
			case ">=" -> opcode == Opcodes.IF_ICMPLT || opcode == Opcodes.IFLT;
			default -> false;
		};
	}
	//endregion
	
	//region Number loading as
	public static void loadNumberAsInt(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(LONG)) {
			visitor.visitInsn(Opcodes.L2I);
		} else if (type.equals(FLOAT)) {
			visitor.visitInsn(Opcodes.F2I);
		} else if (type.equals(DOUBLE)) {
			visitor.visitInsn(Opcodes.D2I);
		}
	}
	
	public static void loadNumberAsLong(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(BYTE) || type.equals(SHORT) || type.equals(INT)) {
			visitor.visitInsn(Opcodes.I2L);
		} else if (type.equals(FLOAT)) {
			visitor.visitInsn(Opcodes.F2L);
		} else if (type.equals(DOUBLE)) {
			visitor.visitInsn(Opcodes.D2L);
		}
	}
	
	public static void loadNumberAsFloat(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(BYTE) || type.equals(SHORT) || type.equals(INT)) {
			visitor.visitInsn(Opcodes.I2F);
		} else if (type.equals(LONG)) {
			visitor.visitInsn(Opcodes.L2F);
		} else if (type.equals(DOUBLE)) {
			visitor.visitInsn(Opcodes.D2F);
		}
	}
	
	public static void loadNumberAsDouble(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(BYTE) || type.equals(SHORT) || type.equals(INT)) {
			visitor.visitInsn(Opcodes.I2D);
		} else if (type.equals(LONG)) {
			visitor.visitInsn(Opcodes.L2D);
		} else if (type.equals(FLOAT)) {
			visitor.visitInsn(Opcodes.F2D);
		}
	}
	//endregion
	
	//region Constants loading
	public static void loadNumber(@NotNull MethodVisitor visitor, @NotNull Number number) {
		if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
			loadInteger(visitor, number.intValue());
		} else if (number instanceof Long l) {
			loadLong(visitor, l);
		} else if (number instanceof Float f) {
			loadFloat(visitor, f);
		} else if (number instanceof Double d) {
			loadDouble(visitor, d);
		}
	}
	
	public static void loadDefaultConst(@NotNull MethodVisitor visitor, @NotNull Type type) {
		if (type.equals(BOOLEAN) || type.equals(CHAR) || type.equals(BYTE) || type.equals(SHORT) || type.equals(INT)) {
			visitor.visitInsn(Opcodes.ICONST_0);
		} else if (type.equals(LONG)) {
			visitor.visitInsn(Opcodes.LCONST_0);
		} else if (type.equals(FLOAT)) {
			visitor.visitInsn(Opcodes.FCONST_0);
		} else if (type.equals(DOUBLE)) {
			visitor.visitInsn(Opcodes.DCONST_0);
		} else {
			visitor.visitInsn(Opcodes.ACONST_NULL);
		}
	}
	//endregion
	
	//region Annotations
	public static void instrumentAnnotation(@NotNull AnnotationVisitor visitor, @NotNull AnnotationData annotation) {
		for (Map.Entry<String, Object> entry : annotation.values().entrySet()) {
			switch (entry.getValue()) {
				case AnnotationData data -> {
					AnnotationVisitor child = visitor.visitAnnotation(entry.getKey(), data.type().getDescriptor());
					instrumentAnnotation(child, data);
					child.visitEnd();
				}
				case Enum<?> value -> visitor.visitEnum(entry.getKey(), Type.getDescriptor(value.getClass()), value.name());
				case List<?> values -> {
					AnnotationVisitor array = visitor.visitArray(entry.getKey());
					if (values.isEmpty()) {
						array.visitEnd();
						continue;
					}
					Object first = values.getFirst();
					if (first instanceof Enum<?> enumValue) {
						values.forEach(value -> array.visitEnum(null, Type.getDescriptor(value.getClass()), enumValue.name()));
					} else {
						values.forEach(value -> array.visit(null, value));
					}
				}
				case null -> {}
				default -> visitor.visit(entry.getKey(), entry.getValue());
			}
		}
		visitor.visitEnd();
	}
	
	public static void instrumentMethodAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodData method) {
		method.getAnnotations().forEach(annotation -> {
			instrumentAnnotation(visitor.visitAnnotation(annotation.type().getDescriptor(), true), annotation);
		});
	}
	
	public static void instrumentParameterAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodData method) {
		method.parameters().forEach(parameter -> {
			parameter.getAnnotations().forEach(annotation -> {
				instrumentAnnotation(visitor.visitParameterAnnotation(parameter.index(), annotation.type().getDescriptor(), true), annotation);
			});
		});
	}
	//endregion
	
	public static void instrumentMethodCall(@NotNull MethodVisitor visitor, @NotNull MethodData method, boolean iface) {
		instrumentMethodCall(visitor, method, iface, 0);
	}
	
	public static void instrumentMethodCall(@NotNull MethodVisitor visitor, @NotNull MethodData method, boolean iface, int index) {
		if (iface && !method.is(TypeModifier.STATIC)) {
			visitor.visitVarInsn(Opcodes.ALOAD, index);
			visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, method.owner().getInternalName(), method.name(), method.type().getDescriptor(), true);
		} else if (method.is(TypeModifier.STATIC)) {
			ClassData data = AgentContext.get().getClassData(method.owner());
			visitor.visitMethodInsn(Opcodes.INVOKESTATIC, method.owner().getInternalName(), method.name(), method.type().getDescriptor(), data.classType() == ClassType.INTERFACE);
		} else {
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, method.owner().getInternalName(), method.name(), method.type().getDescriptor(), false);
		}
	}
	
	public static void instrumentThrownException(@NotNull MethodVisitor visitor, @NotNull Type type, @NotNull String message) {
		visitor.visitTypeInsn(Opcodes.NEW, type.getInternalName());
		visitor.visitInsn(Opcodes.DUP);
		visitor.visitLdcInsn(message);
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "(Ljava/lang/String;)V", false);
		visitor.visitInsn(Opcodes.ATHROW);
	}
	
	public static void instrumentThrownException(@NotNull MethodVisitor visitor, @NotNull Type type, int cause) {
		visitor.visitTypeInsn(Opcodes.NEW, type.getInternalName());
		visitor.visitInsn(Opcodes.DUP);
		visitor.visitVarInsn(Opcodes.ALOAD, cause);
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "(Ljava/lang/Throwable;)V", false);
		visitor.visitInsn(Opcodes.ATHROW);
	}
	
	public static void instrumentPatternCheck(@NotNull MethodVisitor visitor, @NotNull String pattern, int index, @NotNull Label end) {
		visitor.visitLdcInsn(pattern);
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", false);
		visitor.visitVarInsn(Opcodes.ALOAD, index);
		visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
		visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "matches", "()Z", false);
		visitor.visitJumpInsn(Opcodes.IFNE, end);
	}
	
	public static void instrumentNonNullCheck(@NotNull MethodVisitor visitor, @NotNull String message) {
		visitor.visitLdcInsn(message);
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
	}
	
	public static void instrumentFactoryCall(@NotNull MethodVisitor visitor, @NotNull Type factory, @NotNull Type target, @NotNull String value) {
		visitor.visitFieldInsn(Opcodes.GETSTATIC, factory.getInternalName(), "INSTANCE", factory.getDescriptor());
		visitor.visitLdcInsn(target.getDescriptor());
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE.getInternalName(), "getType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;", false);
		visitor.visitLdcInsn(value);
		visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, factory.getInternalName(), "create", "(Lorg/objectweb/asm/Type;Ljava/lang/String;)Ljava/lang/Object;", false);
		visitor.visitTypeInsn(Opcodes.CHECKCAST, target.getInternalName());
	}
	
	public static int newLocal(@NotNull MethodVisitor visitor, @NotNull Type type) {
		if (visitor instanceof LocalVariablesSorter sorter) {
			return sorter.newLocal(type);
		}
		throw new IllegalStateException("LocalVariablesSorter is required as base visitor");
	}
	
	//region Internal
	private static void loadInteger(@NotNull MethodVisitor visitor, int i) {
		if (i >= -1 && i <= 5) {
			visitor.visitInsn(Opcodes.ICONST_0 + i);
		} else if (i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE) {
			visitor.visitIntInsn(Opcodes.BIPUSH, i);
		} else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
			visitor.visitIntInsn(Opcodes.SIPUSH, i);
		} else {
			visitor.visitLdcInsn(i);
		}
	}
	
	private static void loadLong(@NotNull MethodVisitor visitor, long l) {
		if (l == 0L || l == 1L) {
			visitor.visitInsn(Opcodes.LCONST_0 + (int) l);
		} else {
			visitor.visitLdcInsn(l);
		}
	}
	
	@SuppressWarnings("FloatingPointEquality")
	private static void loadFloat(@NotNull MethodVisitor visitor, float f) {
		if (f == 0.0F) {
			visitor.visitInsn(Opcodes.FCONST_0);
		} else if (f == 1.0F) {
			visitor.visitInsn(Opcodes.FCONST_1);
		} else if (f == 2.0F) {
			visitor.visitInsn(Opcodes.FCONST_2);
		} else {
			visitor.visitLdcInsn(f);
		}
	}
	
	@SuppressWarnings("FloatingPointEquality")
	private static void loadDouble(@NotNull MethodVisitor visitor, double d) {
		if (d == 0.0D) {
			visitor.visitInsn(Opcodes.DCONST_0);
		} else if (d == 1.0D) {
			visitor.visitInsn(Opcodes.DCONST_1);
		} else {
			visitor.visitLdcInsn(d);
		}
	}
	//endregion
}
