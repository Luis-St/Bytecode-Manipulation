package net.luis.agent.util;

public enum TargetType {
	
	// <empty string> -> value and mode are ignored
	HEAD,
	
	// NEW, NEWARRAY, MULTIANEWARRAY
	//  Type
	//  Type[]
	//  Type[][]
	NEW,
	
	// INVOKESPECIAL, INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE
	//  Type#method(...)
	INVOKE,
	
	// ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
	// GETSTATIC, GETFIELD
	//  #field
	//  name of parameter/variable (may not work properly)
	//  index of parameter/variable
	ACCESS,
	
	// BALOAD (byte, boolean), CALOAD, SALOAD, IALOAD, LALOAD, FALOAD, DALOAD, AALOAD -> value is ignored
	ACCESS_ARRAY,
	
	// ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
	// PUTSTATIC, PUTFIELD
	//  field
	//  Type#field
	//  name of parameter/variable (may not work properly)
	//  index of parameter/variable
	ASSIGN,
	
	// BASTORE (byte, boolean), CASTORE, SASTORE, IASTORE, LASTORE, FASTORE, DASTORE, AASTORE -> value is ignored
	ASSIGN_ARRAY,
	
	// ACONST_NULL
	// ICONST_M1, ICONST_0 (0 or false), ICONST_1 (1 or true), ICONST_2, ICONST_3, ICONST_4, ICONST_5
	// LCONST_0, LCONST_1
	// FCONST_0, FCONST_1, FCONST_2
	// DCONST_0, DCONST_1
	// BIPUSH + value
	// SIPUSH + value
	// LDC + value
	CONSTANT,
	
	// + IADD, LADD, FADD, DADD
	// - ISUB, LSUB, FSUB, DSUB
	// * IMUL, LMUL, FMUL, DMUL
	// / IDIV, LDIV, FDIV, DDIV
	// % IREM, LREM, FREM, DREM
	// neg INEG, LNEG, FNEG, DNEG
	// & IAND, LAND
	// | IOR, LOR
	// ^ IXOR, LXOR
	// << ISHL, LSHL
	// >> ISHR, LSHR
	// >>> IUSHR, LUSHR
	// ~ ICONST_M1 + IXOR, LCONST_M1 + LXOR
	NUMERIC_OPERAND,
	
	// All the following operations are followed by a jump instruction
	// This means that the condition is inverted
	// == (used for !=)
	//   int, short, byte, char, boolean -> IF_ICMPEQ
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFEQ
	//   Full Array, Object -> IF_ACMPEQ
	//   null -> IFNULL
	// != (used for ==)
	//   int, short, byte, char, boolean -> IF_ICMPNE
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFNE
	//   Full Array, Object -> IF_ACMPNE
	//   null -> IFNONNULL
	// < (used for >=)
	//   int, short, byte, char -> IF_ICMPLT
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFLT
	// <= (used for >)
	//   int, short, byte, char -> IF_ICMPLE
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFLE
	// > (used for <=)
	//   int, short, byte, char -> IF_ICMPGT
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFGT
	// >= (used for <)
	//   int, short, byte, char -> IF_ICMPGE
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFGE
	// instanceof
	//   INSTANCEOF
	COMPARE,
	
	// <empty string> -> value and mode are ignored
	//   RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
	RETURN;
}
