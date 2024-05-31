package net.luis.agent.asm.scanner;

import net.luis.agent.asm.data.*;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class MethodScanner extends MethodVisitor {
	
	private final Method method;
	private final Map<Integer, Map.Entry<String, Set<TypeModifier>>> parameters = new HashMap<>();
	private final Map<Integer, Map<Type, Annotation>> parameterAnnotations = new HashMap<>();
	private int parameterIndex;
	
	public MethodScanner(@NotNull Method method) {
		super(Opcodes.ASM9);
		this.method = method;
	}
	
	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return new AnnotationScanner((name, value) -> this.method.getAnnotationDefault().set(value));
	}
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Annotation annotation = Annotation.builder(Type.getType(descriptor)).visible(visible).build();
		this.method.getAnnotations().put(annotation.getType(), annotation);
		return new AnnotationScanner(annotation.getValues()::put);
	}
	
	@Override
	public void visitParameter(@Nullable String name, int access) {
		if (name == null) {
			name = "arg" + this.parameterIndex;
		}
		this.parameters.put(this.parameterIndex++, Map.entry(name, TypeModifier.fromParameterAccess(access)));
	}
	
	@Override
	public @NotNull AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String descriptor, boolean visible) {
		Annotation annotation = Annotation.builder(Type.getType(descriptor)).visible(visible).build();
		this.parameterAnnotations.computeIfAbsent(parameter, p -> new HashMap<>()).put(annotation.getType(), annotation);
		return new AnnotationScanner(annotation.getValues()::put);
	}
	
	@Override
	public void visitLocalVariable(@NotNull String name, @NotNull String descriptor, @Nullable String genericSignature, @NotNull Label start, @NotNull Label end, int index) {
		int offset = this.parameters.size() + (this.method.is(TypeModifier.STATIC) ? 0 : 1);
		if (index < offset) {
			return;
		}
		this.method.getLocals().put(index, LocalVariable.builder(this.method, index, name, Type.getType(descriptor)).genericSignature(genericSignature).build());
	}
	
	@Override
	public void visitEnd() {
		Type[] types = this.method.getType().getArgumentTypes();
		for (int i = 0; i < types.length; i++) {
			Parameter.Builder builder = Parameter.builder(this.method).index(i).type(types[i]).annotations(this.parameterAnnotations.getOrDefault(i, new HashMap<>()));
			Map.Entry<String, Set<TypeModifier>> entry = this.parameters.getOrDefault(i, Map.entry("arg" + i, EnumSet.noneOf(TypeModifier.class)));
			this.method.getParameters().put(i, builder.name(entry.getKey()).modifiers(entry.getValue()).build());
		}
	}
}
