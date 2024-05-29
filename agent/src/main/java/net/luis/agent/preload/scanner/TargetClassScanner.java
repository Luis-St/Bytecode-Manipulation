package net.luis.agent.preload.scanner;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.TargetMode;
import net.luis.agent.util.TargetType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import static net.luis.agent.asm.Instrumentations.*;

/**
 *
 * @author Luis-St
 *
 */

public class TargetClassScanner extends ClassVisitor {
	
	private final PreloadContext context;
	private final MethodData method;
	private final AnnotationData target;
	private final TargetMode mode;
	private final int offset;
	private TargetMethodScanner visitor;
	
	public TargetClassScanner(@NotNull PreloadContext context, @NotNull MethodData method, @NotNull AnnotationData target) {
		super(Opcodes.ASM9);
		this.context = context;
		this.method = method;
		this.target = target;
		this.mode = TargetMode.valueOf(target.getOrDefault(context, "mode"));
		this.offset = target.getOrDefault(context, "offset");
	}
	
	public boolean visitedTarget() {
		return this.visitor != null;
	}
	
	public int getLine() {
		if (this.visitor == null) {
			return -1;
		}
		int line = this.visitor.getLine();
		if (line == -1) {
			return -1;
		}
		line += this.offset;
		if (this.mode == TargetMode.AFTER) {
			line++;
		}
		int firstLine = this.visitor.getFirstLine();
		int lastLine = this.visitor.getLastLine();
		if (firstLine > line) {
			return firstLine;
		} else if (line > lastLine) {
			return lastLine;
		}
		return line;
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
		if (this.method.getMethodSignature().equals(name + descriptor)) {
			this.visitor = new TargetMethodScanner(this.context, this.method, this.target);
			return this.visitor;
		}
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}
	
	private static class TargetMethodScanner extends MethodVisitor {
		
		private static final String MISSING_INFORMATION = "Missing Debug Information";
		private static final String NOT_FOUND = "Not Found";
		
		private final MethodData method;
		private final String value;
		private final TargetType type;
		private final int ordinal;
		private int lastOpcode = -1;
		private int targetLine = -1;
		private int firstLine = -1;
		private int currentLine;
		private int visited;
		
		private TargetMethodScanner(@NotNull PreloadContext context, @NotNull MethodData method, @NotNull AnnotationData target) {
			super(Opcodes.ASM9);
			this.method = method;
			this.value = target.getOrDefault(context, "value");
			this.type = TargetType.valueOf(target.get("type"));
			this.ordinal = target.getOrDefault(context, "ordinal");
		}
		
		private void target() {
			if (this.visited == this.ordinal && this.targetLine == -1) {
				this.targetLine = this.currentLine;
			} else {
				this.visited++;
			}
		}
		
		public int getFirstLine() {
			return this.firstLine;
		}
		
		public int getLastLine() {
			return this.currentLine;
		}
		
		public int getLine() {
			return this.targetLine;
		}
		
		@Override
		public void visitLineNumber(int line, @NotNull Label label) {
			this.currentLine = line;
			if (this.firstLine == -1) {
				this.firstLine = line;
			}
			if (this.type == TargetType.HEAD && this.targetLine == -1) {
				this.targetLine = this.currentLine;
			}
		}
		
		// INVOKE -> INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE
		@Override
		public void visitMethodInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = opcode;
			if (opcode == Opcodes.INVOKESPECIAL && "<init>".equals(name)) {
				return;
			}
			if (this.type == TargetType.INVOKE && ASMUtils.matchesTarget(this.value, Type.getObjectType(owner), name, Type.getType(descriptor))) {
				this.target();
			}
		}
		
		// CONSTANT
		@Override
		public void visitInvokeDynamicInsn(@NotNull String name, @NotNull String descriptor, @NotNull Handle handle, Object... arguments) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = Opcodes.INVOKEDYNAMIC;
			if (this.type != TargetType.CONSTANT) {
				return;
			}
			for (Object argument : arguments) {
				if (argument instanceof String str) {
					String[] parts = str.split("\\u0001");
					for (String part : parts) {
						if (this.value.equals(part)) {
							this.target();
						}
					}
				}
			}
		}
		
		// ACCESS -> GETSTATIC, GETFIELD
		// ASSIGN -> PUTSTATIC, PUTFIELD
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = opcode;
			String value = this.value;
			if (value.startsWith("#")) {
				value = value.substring(1);
			}
			if (this.type == TargetType.ACCESS && (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD)) {
				if (ASMUtils.matchesTarget(value, Type.getObjectType(owner), name, Type.getType(descriptor))) {
					this.target();
				}
			} else if (this.type == TargetType.ASSIGN && (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD)) {
				if (ASMUtils.matchesTarget(value, Type.getObjectType(owner), name, Type.getType(descriptor))) {
					this.target();
				}
			}
		}
		
		// ACCESS -> ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
		// ASSIGN -> ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
		@Override
		public void visitVarInsn(int opcode, int index) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = opcode;
			if (!this.value.contains("#")) {
				if (this.type == TargetType.ACCESS && isLoad(opcode)) {
					this.checkVariableIndex(index);
				} else if (this.type == TargetType.ASSIGN && isStore(opcode)) {
					this.checkVariableIndex(index);
				}
			}
		}
		
		// CONSTANT -> ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1
		// RETURN -> RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
		// NUMERIC_OPERAND -> (ILFD)ADD, (ILFD)SUB, (ILFD)MUL, (ILFD)DIV, (ILFD)REM, (ILFD)NEG, (IL)AND, (IL)OR, (IL)XOR, (IL)SHL, (IL)SHR, (IL)USHR
		// ACCESS_ARRAY -> IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD
		// ASSIGN_ARRAY -> IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE
		@Override
		public void visitInsn(int opcode) {
			if (this.targetLine != -1) {
				return;
			}
			if (this.type == TargetType.CONSTANT && isConstant(opcode, this.value)) {
				this.target();
			} else if (this.type == TargetType.RETURN && isReturn(opcode)) {
				this.target();
			} else if (this.type == TargetType.NUMERIC_OPERAND && isNumericOperand(opcode, this.lastOpcode, this.value)) {
				this.target();
			} else if (this.type == TargetType.ACCESS_ARRAY && isArrayLoad(opcode)) {
				this.target();
			} else if (this.type == TargetType.ASSIGN_ARRAY && isArrayStore(opcode)) {
				this.target();
			}
			this.lastOpcode = opcode;
		}
		
		// CONSTANT -> BIPUSH, SIPUSH
		// NEW -> NEWARRAY
		@Override
		public void visitIntInsn(int opcode, int operand) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = opcode;
			if (this.type == TargetType.CONSTANT && (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH)) {
				if (this.value.chars().allMatch(Character::isDigit) && operand == Integer.parseInt(this.value)) {
					this.target();
				}
			} else if (this.type == TargetType.NEW && opcode == Opcodes.NEWARRAY && this.value.equalsIgnoreCase(this.arrayOpcodeToString(operand))) {
				this.target();
			}
		}
		
		// CONSTANT
		@Override
		public void visitLdcInsn(@NotNull Object value) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = Opcodes.LDC;
			if (this.type != TargetType.CONSTANT) {
				return;
			}
			if (value instanceof Number number) {
				if (this.isNumber(this.value)) {
					if (this.matchNumber(number, this.value, '\0')) {
						this.target();
					}
				} else {
					char type = this.value.charAt(this.value.length() - 1);
					String remaining = this.value.substring(0, this.value.length() - 1);
					if (this.isNumber(remaining) && this.matchNumber(number, remaining, type)) {
						this.target();
					}
				}
			} else if (this.value.equals(value.toString())) {
				this.target();
			}
		}
		
		// NEW -> NEW, ANEWARRAY
		// COMPARE -> INSTANCEOF
		@Override
		public void visitTypeInsn(int opcode, @NotNull String type) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = opcode;
			if (this.type == TargetType.NEW) {
				Type objectType = Type.getObjectType(type);
				if (opcode == Opcodes.NEW && ASMUtils.isSameType(objectType, this.value)) {
					this.target();
				} else if (opcode == Opcodes.ANEWARRAY && ASMUtils.isSameType(Type.getType("[" + objectType.getDescriptor()), this.value)) {
					this.target();
				}
			} else if (this.type == TargetType.COMPARE && opcode == Opcodes.INSTANCEOF) {
				this.target();
			}
		}
		
		// NEW -> MULTIANEWARRAY
		@Override
		public void visitMultiANewArrayInsn(@NotNull String descriptor, int numDimensions) {
			if (this.targetLine != -1) {
				return;
			}
			this.lastOpcode = Opcodes.MULTIANEWARRAY;
			if (this.type == TargetType.NEW && ASMUtils.isSameType(Type.getType(descriptor), this.value)) {
				this.target();
			}
		}
		
		// COMPARE -> IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, IFNULL, IFNONNULL, IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
		@Override
		public void visitJumpInsn(int opcode, @NotNull Label label) {
			if (this.targetLine != -1) {
				return;
			}
			if (this.type == TargetType.COMPARE && isCompare(this.value, opcode)) {
				this.target();
			}
			this.lastOpcode = opcode;
		}
		
		//region Helper methods
		private void checkVariableIndex(int index) {
			if (this.value.chars().allMatch(Character::isDigit)) {
				if (index == Integer.parseInt(this.value)) {
					this.target();
				}
			} else if (this.method.isLocalVariable(index)) {
				if (this.method.localVariables().isEmpty()) {
					throw CrashReport.create("Unable to find local variable by name, because the local variable name was not included into the class file during compilation", MISSING_INFORMATION)
						.addDetail("Method", this.method.getMethodSignature()).exception();
				}
				LocalVariableData local = this.method.getLocalVariable(index);
				if (local == null) {
					throw CrashReport.create("Local Variable not found", NOT_FOUND).addDetail("Method", this.method.getMethodSignature())
						.addDetail("Local Variable Index", index).addDetail("Local Variable Indexes", this.method.localVariables().keySet()).exception();
				}
				if (local.name().equals(this.value)) {
					this.target();
				}
			} else {
				ParameterData parameter = this.method.parameters().get(this.method.is(TypeModifier.STATIC) ? index : index - 1);
				if (parameter == null) {
					throw CrashReport.create("Parameter not found", NOT_FOUND).addDetail("Method", this.method.getMethodSignature())
						.addDetail("Parameter Index", index).addDetail("Parameter Indexes", this.method.parameters().stream().map(ParameterData::index).toList()).exception();
				}
				if (!parameter.isNamed()) {
					throw CrashReport.create("Unable to find parameter by name, because the parameter name was not included into the class file during compilation", MISSING_INFORMATION).addDetail("Method", this.method.getMethodSignature())
						.addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
				}
				if (parameter.name().equals(this.value)) {
					this.target();
				}
			}
		}
		
		private @NotNull String arrayOpcodeToString(int opcode) {
			return switch (opcode) {
				case Opcodes.T_BOOLEAN -> "boolean[]";
				case Opcodes.T_BYTE -> "byte[]";
				case Opcodes.T_CHAR -> "char[]";
				case Opcodes.T_SHORT -> "short[]";
				case Opcodes.T_INT -> "int[]";
				case Opcodes.T_LONG -> "long[]";
				case Opcodes.T_FLOAT -> "float[]";
				case Opcodes.T_DOUBLE -> "double[]";
				default -> throw new IllegalStateException("Unexpected opcode: " + opcode);
			};
		}
		
		private boolean isNumber(@NotNull String value) {
			boolean dot = false;
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				if (c == '.') {
					if (dot) {
						return false;
					}
					dot = true;
				} else if (!Character.isDigit(c)) {
					return false;
				}
			}
			return true;
		}
		
		@SuppressWarnings("FloatingPointEquality")
		private boolean matchNumber(@NotNull Number number, @NotNull String value, char type) {
			return switch (type) {
				case 'b', 'B' -> number.byteValue() == Byte.parseByte(value);
				case 's', 'S' -> number.shortValue() == Short.parseShort(value);
				case 'i', 'I' -> number.intValue() == Integer.parseInt(value);
				case 'l', 'L' -> number.longValue() == Long.parseLong(value);
				case 'f', 'F' -> number.floatValue() == Float.parseFloat(value);
				case 'd', 'D' -> number.doubleValue() == Double.parseDouble(value);
				case '\0' -> {
					if (value.contains(".")) {
						yield number.doubleValue() == Double.parseDouble(value);
					} else {
						yield number.longValue() == Long.parseLong(value);
					}
				}
				default -> false;
			};
		}
		//endregion
	}
}
