package net.luis.agent.util;

public enum TargetType {
	
	// <empty string>
	HEAD,
	
	// IllegalStateException#<init>(String)
	NEW,
	
	// INVOKESPECIAL (exclude constructor calls), INVOKESTATIC, INVOKEVIRTUAL, INVOKEINTERFACE
	INVOKE,
	
	// ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
	// GETSTATIC, GETFIELD
	ACCESS,
	
	// IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD
	ACCESS_ARRAY,
	
	// ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
	// PUTSTATIC, PUTFIELD
	ASSIGN,
	
	// IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE
	ASSIGN_ARRAY,
	
	// ACONST_NULL
	// ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5
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
	
	// ==
	//   int, short, byte, char, boolean -> IF_ICMPEQ
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFEQ
	//   Full Array, Object -> IF_ACMPEQ
	//   null -> IFNULL
	// !=
	//   int, short, byte, char, boolean -> IF_ICMPNE
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFNE
	//   Full Array, Object -> IF_ACMPNE
	//   null -> IFNONNULL
	// <
	//   int, short, byte, char -> IF_ICMPLT
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFLT
	// <=
	//   int, short, byte, char -> IF_ICMPLE
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFLE
	// >
	//   int, short, byte, char -> IF_ICMPGT
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFGT
	// >=
	//   int, short, byte, char -> IF_ICMPGE
	//   long, float, double -> LCMP, FCMPL, FCMPG, DCMPL, or DCMPG + IFGE
	// instanceof
	COMPARE,
	
	// return IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
	RETURN;
}
