package net.luis.agent.asm.generation;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;
import static net.luis.agent.asm.generation.GenerationUtils.*;

/**
 *
 * @author Luis-St
 *
 */

public class RuntimeUtilsGenerator extends Generator {
	
	public RuntimeUtilsGenerator() {
		super(RUNTIME_UTILS.getClassName());
	}
	
	@Override
	public void generate(@NotNull ClassVisitor cv) {
		cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, RUNTIME_UTILS.getInternalName(), null, "java/lang/Object", null);
		generateDefaultConstructor(cv);
		this.generateIsAccessAllowed(cv);
		cv.visitEnd();
	}
	
	@SuppressWarnings("DuplicatedCode")
	private void generateIsAccessAllowed(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "isAccessAllowed", "(Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)Z", null, null);
		//region Labels
		Label methodStart = new Label();
		Label[] labels = new Label[] {
			new Label(), new Label(), new Label(), new Label(), new Label(), new Label(), new Label()
		};
		Label methodEnd = new Label();
		//endregion
		
		//region Parameters
		mv.visitParameter("target", 0);
		mv.visitParameterAnnotation(0, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitParameter("pattern", 0);
		mv.visitParameter("className", 0);
		mv.visitParameterAnnotation(2, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitParameter("methodName", 0);
		mv.visitParameterAnnotation(3, NOT_NULL.getDescriptor(), false).visitEnd();
		//endregion
		
		mv.visitCode();
		mv.visitLabel(methodStart);
		
		//region Parameter validation
		instrumentNonNullCheck(mv, 0, "Target must not be null");
		mv.visitInsn(Opcodes.POP);
		instrumentNonNullCheck(mv, 2, "Class name must not be null");
		mv.visitInsn(Opcodes.POP);
		instrumentNonNullCheck(mv, 3, "Method name must not be null");
		mv.visitInsn(Opcodes.POP);
		mv.visitLabel(labels[0]);
		//endregion
		
		//region Create class and method concat
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitLdcInsn("#");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
		mv.visitVarInsn(Opcodes.ALOAD, 3);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
		mv.visitVarInsn(Opcodes.ASTORE, 4);
		//endregion
		
		mv.visitVarInsn(Opcodes.ILOAD, 1);
		mv.visitJumpInsn(Opcodes.IFEQ, labels[1]);
		
		//region Pattern check
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 4);
		mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/regex/Pattern", "matches", "(Ljava/lang/String;Ljava/lang/CharSequence;)Z", false);
		mv.visitInsn(Opcodes.IRETURN);
		//endregion
		
		mv.visitLabel(labels[1]);
		
		//region Equals check
		instrumentEqualsIgnoreCase(mv, 0, 2);
		mv.visitJumpInsn(Opcodes.IFNE, labels[2]);
		
		instrumentEqualsIgnoreCase(mv, 0, 3);
		mv.visitJumpInsn(Opcodes.IFNE, labels[2]);
		
		instrumentEqualsIgnoreCase(mv, 0, 4);
		mv.visitJumpInsn(Opcodes.IFNE, labels[2]);
		
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitLdcInsn("#");
		mv.visitVarInsn(Opcodes.ALOAD, 3);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(Opcodes.IFEQ, labels[3]);
		//endregion
		
		mv.visitLabel(labels[2]);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IRETURN);
		mv.visitLabel(labels[3]);
		
		//region Simple name equals check
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitVarInsn(Opcodes.ALOAD, 2);
		mv.visitLdcInsn(".");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "lastIndexOf", "(Ljava/lang/String;)I", false);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitInsn(Opcodes.IADD);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "substring", "(I)Ljava/lang/String;", false);
		mv.visitVarInsn(Opcodes.ASTORE, 5);
		
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 5);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(Opcodes.IFNE, labels[4]);
		
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 5);
		mv.visitLdcInsn("#");
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
		mv.visitVarInsn(Opcodes.ALOAD, 3);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equalsIgnoreCase", "(Ljava/lang/String;)Z", false);
		mv.visitJumpInsn(Opcodes.IFEQ, labels[5]);
		
		mv.visitLabel(labels[4]);
		mv.visitInsn(Opcodes.ICONST_1);
		mv.visitJumpInsn(Opcodes.GOTO, labels[6]);
		mv.visitLabel(labels[5]);
		mv.visitInsn(Opcodes.ICONST_0);
		mv.visitLabel(labels[6]);
		mv.visitInsn(Opcodes.IRETURN);
		//endregion
		
		mv.visitLabel(methodEnd);
		mv.visitLocalVariable("target", STRING.getDescriptor(), null, methodStart, methodEnd, 0);
		mv.visitLocalVariable("pattern", BOOLEAN.getDescriptor(), null, methodStart, methodEnd, 1);
		mv.visitLocalVariable("className", STRING.getDescriptor(), null, methodStart, methodEnd, 2);
		mv.visitLocalVariable("methodName", STRING.getDescriptor(), null, methodStart, methodEnd, 3);
		mv.visitLocalVariable("concat", STRING.getDescriptor(), null, labels[0], methodEnd, 4);
		mv.visitLocalVariable("simple", STRING.getDescriptor(), null, labels[3], methodEnd, 5);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
