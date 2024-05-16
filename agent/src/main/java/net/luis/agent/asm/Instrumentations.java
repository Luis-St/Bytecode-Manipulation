package net.luis.agent.asm;

import net.luis.agent.preload.data.AnnotationData;
import net.luis.agent.preload.data.MethodData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public interface Instrumentations {
	
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
				case null -> {break;}
				default -> visitor.visit(entry.getKey(), entry.getValue());
			}
		}
		visitor.visitEnd();
	}
	
	default void instrumentMethodAnnotations(@NotNull MethodVisitor visitor, @NotNull MethodData method, boolean generated) {
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
	
	default void instrumentThrownException(@NotNull MethodVisitor visitor, @NotNull Type type, @NotNull String message) {
		visitor.visitTypeInsn(Opcodes.NEW, type.getInternalName());
		visitor.visitInsn(Opcodes.DUP);
		visitor.visitLdcInsn(message);
		visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", "(Ljava/lang/String;)V", false);
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
}
