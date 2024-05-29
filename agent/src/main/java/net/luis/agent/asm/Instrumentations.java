package net.luis.agent.asm;

import net.luis.agent.preload.data.AnnotationData;
import net.luis.agent.preload.data.MethodData;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public interface Instrumentations {
	
	String METAFACTORY_DESCRIPTOR = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;";
	Handle METAFACTORY_HANDLE = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory", METAFACTORY_DESCRIPTOR, false);
	
	//region Internal
	private void loadInteger(@NotNull MethodVisitor visitor, int i) {
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
	
	private void loadLong(@NotNull MethodVisitor visitor, long l) {
		if (l == 0L || l == 1L) {
			visitor.visitInsn(Opcodes.LCONST_0 + (int) l);
		} else {
			visitor.visitLdcInsn(l);
		}
	}
	
	@SuppressWarnings("FloatingPointEquality")
	private void loadFloat(@NotNull MethodVisitor visitor, float f) {
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
	private void loadDouble(@NotNull MethodVisitor visitor, double d) {
		if (d == 0.0D) {
			visitor.visitInsn(Opcodes.DCONST_0);
		} else if (d == 1.0D) {
			visitor.visitInsn(Opcodes.DCONST_1);
		} else {
			visitor.visitLdcInsn(d);
		}
	}
	//endregion
	
	//region Opcodes
	default boolean isLoad(int opcode) {
		return Opcodes.ALOAD >= opcode && opcode >= Opcodes.ILOAD;
	}
	
	default boolean isArrayLoad(int opcode) {
		return Opcodes.SALOAD >= opcode && opcode >= Opcodes.IALOAD;
	}
	
	default boolean isStore(int opcode) {
		return Opcodes.ASTORE >= opcode && opcode >= Opcodes.ISTORE;
	}
	
	default boolean isArrayStore(int opcode) {
		return Opcodes.SASTORE >= opcode && opcode >= Opcodes.IASTORE;
	}
	
	default boolean isConst(int opcode) {
		return Opcodes.DCONST_1 >= opcode && opcode >= Opcodes.ACONST_NULL;
	}
	
	default boolean isReturn(int opcode) {
		return Opcodes.RETURN >= opcode && opcode >= Opcodes.IRETURN;
	}
	
	default boolean isNumericOperand(int opcode) {
		return Opcodes.LXOR >= opcode && opcode >= Opcodes.IADD;
	}
	
	default boolean isNumericOperand(int opcode, int lastOpcode, @NotNull String value) {
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
	
	default boolean isConstant(int opcode, @NotNull String value) {
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
	
	default boolean isCompare(@NotNull String compare, int opcode) {
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
	default void loadNumberAsInt(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.LONG_TYPE)) {
			visitor.visitInsn(Opcodes.L2I);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			visitor.visitInsn(Opcodes.F2I);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			visitor.visitInsn(Opcodes.D2I);
		}
	}
	
	default void loadNumberAsLong(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			visitor.visitInsn(Opcodes.I2L);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			visitor.visitInsn(Opcodes.F2L);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			visitor.visitInsn(Opcodes.D2L);
		}
	}
	
	default void loadNumberAsFloat(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			visitor.visitInsn(Opcodes.I2F);
		} else if (type.equals(Type.LONG_TYPE)) {
			visitor.visitInsn(Opcodes.L2F);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			visitor.visitInsn(Opcodes.D2F);
		}
	}
	
	default void loadNumberAsDouble(@NotNull MethodVisitor visitor, @NotNull Type type, int index) {
		visitor.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
		if (type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			visitor.visitInsn(Opcodes.I2D);
		} else if (type.equals(Type.LONG_TYPE)) {
			visitor.visitInsn(Opcodes.L2D);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			visitor.visitInsn(Opcodes.F2D);
		}
	}
	//endregion
	
	//region Constants loading
	default void loadNumber(@NotNull MethodVisitor visitor, @NotNull Number number) {
		if (number instanceof Byte || number instanceof Short || number instanceof Integer) {
			this.loadInteger(visitor, number.intValue());
		} else if (number instanceof Long l) {
			this.loadLong(visitor, l);
		} else if (number instanceof Float f) {
			this.loadFloat(visitor, f);
		} else if (number instanceof Double d) {
			this.loadDouble(visitor, d);
		}
	}
	
	default void loadDefaultConst(@NotNull MethodVisitor visitor, @NotNull Type type) {
		if (type.equals(Type.BOOLEAN_TYPE) || type.equals(Type.CHAR_TYPE) || type.equals(Type.BYTE_TYPE) || type.equals(Type.SHORT_TYPE) || type.equals(Type.INT_TYPE)) {
			visitor.visitInsn(Opcodes.ICONST_0);
		} else if (type.equals(Type.LONG_TYPE)) {
			visitor.visitInsn(Opcodes.LCONST_0);
		} else if (type.equals(Type.FLOAT_TYPE)) {
			visitor.visitInsn(Opcodes.FCONST_0);
		} else if (type.equals(Type.DOUBLE_TYPE)) {
			visitor.visitInsn(Opcodes.DCONST_0);
		} else {
			visitor.visitInsn(Opcodes.ACONST_NULL);
		}
	}
	//endregion
	
	//region Annotations
	default void instrumentAnnotation(@NotNull AnnotationVisitor visitor, @NotNull AnnotationData annotation) {
		for (Map.Entry<String, Object> entry : annotation.values().entrySet()) {
			switch (entry.getValue()) {
				case AnnotationData data -> {
					AnnotationVisitor child = visitor.visitAnnotation(entry.getKey(), data.type().getDescriptor());
					this.instrumentAnnotation(child, data);
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
	
	default void instrumentMethodAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodData method) {
		method.getAnnotations().forEach(annotation -> {
			this.instrumentAnnotation(visitor.visitAnnotation(annotation.type().getDescriptor(), true), annotation);
		});
	}
	
	default void instrumentParameterAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodData method) {
		method.parameters().forEach(parameter -> {
			parameter.getAnnotations().forEach(annotation -> {
				this.instrumentAnnotation(visitor.visitParameterAnnotation(parameter.index(), annotation.type().getDescriptor(), true), annotation);
			});
		});
	}
	//endregion
	
	default void instrumentMethodCall(@NotNull MethodVisitor visitor, @NotNull MethodData method, boolean iface) {
		this.instrumentMethodCall(visitor, method, iface, 0);
	}
	
	default void instrumentMethodCall(@NotNull MethodVisitor visitor, @NotNull MethodData method, boolean iface, int index) {
		if (iface && !method.is(TypeModifier.STATIC)) {
			visitor.visitVarInsn(Opcodes.ALOAD, index);
			visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, method.owner().getInternalName(), method.name(), method.type().getDescriptor(), true);
		} else if (method.is(TypeModifier.STATIC)) {
			visitor.visitMethodInsn(Opcodes.INVOKESTATIC, method.owner().getInternalName(), method.name(), method.type().getDescriptor(), false);
		} else {
			visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, method.owner().getInternalName(), method.name(), method.type().getDescriptor(), false);
		}
	}
	
	default void instrumentThrownException(@NotNull MethodVisitor visitor, @NotNull Type type, @NotNull String message) {
		visitor.visitTypeInsn(Opcodes.NEW, type.getInternalName());
		visitor.visitInsn(Opcodes.DUP);
		visitor.visitLdcInsn(message);
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "(Ljava/lang/String;)V", false);
		visitor.visitInsn(Opcodes.ATHROW);
	}
	
	default void instrumentThrownException(@NotNull MethodVisitor visitor, @NotNull Type type, int cause) {
		visitor.visitTypeInsn(Opcodes.NEW, type.getInternalName());
		visitor.visitInsn(Opcodes.DUP);
		visitor.visitVarInsn(Opcodes.ALOAD, cause);
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "(Ljava/lang/Throwable;)V", false);
		visitor.visitInsn(Opcodes.ATHROW);
	}
	
	default void instrumentPatternCheck(@NotNull MethodVisitor visitor, @NotNull String pattern, int index, @NotNull Label end) {
		visitor.visitLdcInsn(pattern);
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "compile", "(Ljava/lang/String;)Ljava/util/regex/Pattern;", false);
		visitor.visitVarInsn(Opcodes.ALOAD, index);
		visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Pattern", "matcher", "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;", false);
		visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/regex/Matcher", "matches", "()Z", false);
		visitor.visitJumpInsn(Opcodes.IFNE, end);
	}
	
	default void instrumentNonNullCheck(@NotNull MethodVisitor visitor, @NotNull String message) {
		visitor.visitLdcInsn(message);
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Objects", "requireNonNull", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false);
	}
	
	default void instrumentFactoryCall(@NotNull MethodVisitor visitor, @NotNull Type factory, @NotNull Type target, @NotNull String value) {
		visitor.visitFieldInsn(Opcodes.GETSTATIC, factory.getInternalName(), "INSTANCE", factory.getDescriptor());
		visitor.visitLdcInsn(target.getDescriptor());
		visitor.visitMethodInsn(Opcodes.INVOKESTATIC, TYPE.getInternalName(), "getType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;", false);
		visitor.visitLdcInsn(value);
		visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, factory.getInternalName(), "create", "(Lorg/objectweb/asm/Type;Ljava/lang/String;)Ljava/lang/Object;", false);
		visitor.visitTypeInsn(Opcodes.CHECKCAST, target.getInternalName());
	}
}
