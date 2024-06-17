package net.luis.agent.asm.generation.generators;

import net.luis.agent.asm.generation.Generator;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

public class MemorizedSupplierGenerator extends Generator {
	
	private static final String CLASS_SIGNATURE = "<X:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/function/Supplier<TX;>;";
	private static final String FIELD_SIGNATURE = "Ljava/util/function/Supplier<TX;>;";
	private static final String CONSTRUCTOR_SIGNATURE = "(Ljava/util/function/Supplier<TX;>;)V";
	private static final String GET_SIGNATURE = "()TX;";
	
	private static final String THIS_VARIABLE_SIGNATURE = "L" + MEMORIZED_SUPPLIER.getInternalName() + "<TX;>;";
	
	public MemorizedSupplierGenerator() {
		super(MEMORIZED_SUPPLIER.getClassName());
	}
	
	@Override
	public void generate(@NotNull ClassVisitor cv) {
		cv.visit(CLASS_VERSION, Opcodes.ACC_PUBLIC, MEMORIZED_SUPPLIER.getInternalName(), CLASS_SIGNATURE, "java/lang/Object", new String[] { "java/util/function/Supplier" });
		cv.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "supplier", "Ljava/util/function/Supplier;", FIELD_SIGNATURE, null).visitEnd();
		cv.visitField(Opcodes.ACC_PRIVATE, "value", "Ljava/lang/Object;", "TX;", null).visitEnd();
		this.generateConstructor(cv);
		this.generateGet(cv);
	}
	
	private void generateConstructor(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/util/function/Supplier;)V", CONSTRUCTOR_SIGNATURE, null);
		Label start = new Label();
		Label end = new Label();
		mv.visitParameter("supplier", 0);
		mv.visitParameterAnnotation(0, NOT_NULL.getDescriptor(), false).visitEnd();
		mv.visitCode();
		mv.visitLabel(start);
		instrumentNonNullCheck(mv, 0, "Supplier must not be null");
		mv.visitInsn(Opcodes.POP);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitFieldInsn(Opcodes.PUTFIELD, MEMORIZED_SUPPLIER.getInternalName(), "supplier", "Ljava/util/function/Supplier;");
		mv.visitInsn(Opcodes.RETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", MEMORIZED_SUPPLIER.getDescriptor(), THIS_VARIABLE_SIGNATURE, start, end, 0);
		mv.visitLocalVariable("supplier", "Ljava/util/function/Supplier;", FIELD_SIGNATURE, start, end, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
	
	private void generateGet(@NotNull ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "get", "()Ljava/lang/Object;", GET_SIGNATURE, null);
		Label start = new Label();
		Label jump = new Label();
		Label end = new Label();
		mv.visitCode();
		mv.visitLabel(start);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, MEMORIZED_SUPPLIER.getInternalName(), "value", "Ljava/lang/Object;");
		mv.visitJumpInsn(Opcodes.IFNONNULL, jump);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, MEMORIZED_SUPPLIER.getInternalName(), "supplier", "Ljava/util/function/Supplier;");
		mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/function/Supplier", "get", "()Ljava/lang/Object;", true);
		mv.visitFieldInsn(Opcodes.PUTFIELD, MEMORIZED_SUPPLIER.getInternalName(), "value", "Ljava/lang/Object;");
		mv.visitLabel(jump);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitFieldInsn(Opcodes.GETFIELD, MEMORIZED_SUPPLIER.getInternalName(), "value", "Ljava/lang/Object;");
		mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Object");
		mv.visitInsn(Opcodes.ARETURN);
		mv.visitLabel(end);
		mv.visitLocalVariable("this", MEMORIZED_SUPPLIER.getDescriptor(), THIS_VARIABLE_SIGNATURE, start, end, 0);
		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}
}
