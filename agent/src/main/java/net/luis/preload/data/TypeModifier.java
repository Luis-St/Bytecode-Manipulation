package net.luis.preload.data;

import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public enum TypeModifier {
	
	STATIC(Opcodes.ACC_STATIC, true, true, true, false, false),
	FINAL(Opcodes.ACC_FINAL, true, true, true, true, false),
	SYNCHRONIZED(Opcodes.ACC_SYNCHRONIZED, false, false, true, false, false),
	OPEN(Opcodes.ACC_OPEN, false, false, false, false, true),
	TRANSITIVE(Opcodes.ACC_TRANSITIVE, false, false, false, false, true),
	VOLATILE(Opcodes.ACC_VOLATILE, false, true, false, false, false),
	BRIDGE(Opcodes.ACC_BRIDGE, false, false, true, false, false),
	STATIC_PHASE(Opcodes.ACC_STATIC_PHASE, false, false, false, false, true),
	VARARGS(Opcodes.ACC_VARARGS, false, false, true, false, false),
	TRANSIENT(Opcodes.ACC_TRANSIENT, false, true, false, false, false),
	NATIVE(Opcodes.ACC_NATIVE, false, false, true, false, false),
	ABSTRACT(Opcodes.ACC_ABSTRACT, true, false, true, false, false),
	STRICT(Opcodes.ACC_STRICT, false, false, true, false, false),
	SYNTHETIC(Opcodes.ACC_SYNTHETIC, true, true, true, true, true),
	MANDATED(Opcodes.ACC_MANDATED, false, false, false, true, false),
	DEPRECATED(Opcodes.ACC_DEPRECATED, true, true, true, true, true);
	
	private final int value;
	private final boolean clazz;
	private final boolean field;
	private final boolean method;
	private final boolean parameter;
	private final boolean module;
	
	TypeModifier(int value, boolean clazz, boolean field, boolean method, boolean parameter, boolean module) {
		this.value = value;
		this.clazz = clazz;
		this.field = field;
		this.method = method;
		this.parameter = parameter;
		this.module = module;
	}
	
	public static List<TypeModifier> fromClassAccess(int access) {
		return fromAccess(access).stream().filter(TypeModifier::allowedOnClass).collect(Collectors.toList());
	}
	
	public static List<TypeModifier> fromFieldAccess(int access) {
		return fromAccess(access).stream().filter(TypeModifier::allowedOnField).collect(Collectors.toList());
	}
	
	public static List<TypeModifier> fromMethodAccess(int access) {
		return fromAccess(access).stream().filter(TypeModifier::allowedOnMethod).collect(Collectors.toList());
	}
	
	public static List<TypeModifier> fromParameterAccess(int access) {
		return fromAccess(access).stream().filter(TypeModifier::allowedOnParameter).collect(Collectors.toList());
	}
	
	public static List<TypeModifier> fromModuleAccess(int access) {
		return fromAccess(access).stream().filter(TypeModifier::allowedOnModule).collect(Collectors.toList());
	}
	
	public static List<TypeModifier> fromAccess(int access) {
		List<TypeModifier> modifiers = new ArrayList<>();
		for (TypeModifier modifier : values()) {
			if ((access & modifier.value) != 0) {
				modifiers.add(modifier);
			}
		}
		return modifiers;
	}
	
	public boolean allowedOnClass() {
		return this.clazz;
	}
	
	public boolean allowedOnField() {
		return this.field;
	}
	
	public boolean allowedOnMethod() {
		return this.method;
	}
	
	public boolean allowedOnParameter() {
		return this.parameter;
	}
	
	public boolean allowedOnModule() {
		return this.module;
	}
}
