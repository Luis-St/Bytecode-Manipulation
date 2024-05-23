package net.luis.agent.asm;

import net.luis.agent.preload.data.AnnotationData;
import net.luis.agent.preload.data.MethodData;
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
