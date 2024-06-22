package net.luis.agent.asm.scanner;

import net.luis.agent.asm.data.*;
import net.luis.agent.asm.type.*;
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
	
	private final Type superType;
	private final Method method;
	private final Map<Integer, Map.Entry<String, Set<TypeModifier>>> parameters = new HashMap<>();
	private final Map<Integer, Map<Type, Annotation>> parameterAnnotations = new HashMap<>();
	private final Map</*Size: 3*/int[], Map<Type, Annotation>> localAnnotations = new HashMap<>();
	private final List<Label> labels = new LinkedList<>();
	private int parameterIndex;
	private boolean primary;
	
	public MethodScanner(@NotNull Type superType, @NotNull Method method) {
		super(Opcodes.ASM9);
		this.superType = superType;
		this.method = method;
	}
	
	@Override
	public void visitLabel(@NotNull Label label) {
		this.labels.add(label);
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
	public AnnotationVisitor visitTypeAnnotation(int typeRef, @Nullable TypePath typePath, @NotNull String descriptor, boolean visible) {
		TypeReference reference = new TypeReference(typeRef);
		if (reference.getSort() == TypeReference.METHOD_FORMAL_PARAMETER) {
			int index = reference.getFormalParameterIndex();
			Annotation annotation = Annotation.builder(Type.getType(descriptor)).visible(visible).build();
			if (!this.parameterAnnotations.getOrDefault(index, new HashMap<>()).containsKey(annotation.getType())) {
				this.parameterAnnotations.computeIfAbsent(index, i -> new HashMap<>()).put(annotation.getType(), annotation);
			}
		}
		return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
	}
	
	@Override
	public void visitMethodInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
		if (this.method.is(MethodType.CONSTRUCTOR) && opcode == Opcodes.INVOKESPECIAL && this.superType.getInternalName().equals(owner) && "<init>".equals(name)) {
			this.method.makeConstructorPrimary();
		}
	}
	
	@Override
	public void visitLocalVariable(@NotNull String name, @NotNull String descriptor, @Nullable String genericSignature, @NotNull Label start, @NotNull Label end, int index) {
		int offset = this.parameters.size() + (this.method.is(TypeModifier.STATIC) ? 0 : 1);
		if (index < offset) {
			return;
		}
		int s = Math.max(0, this.labels.indexOf(start) - 1); // Start label is the next label after the declaration
		int e = this.labels.indexOf(end);
		this.method.getLocals().add(LocalVariable.builder(this.method, index, name, Type.getType(descriptor)).genericSignature(genericSignature).bounds(s, e).build());
	}
	
	@Override
	public @Nullable AnnotationVisitor visitLocalVariableAnnotation(int typeRef, @Nullable TypePath typePath, Label @NotNull [] start, Label @NotNull [] end, int @NotNull [] index, @NotNull String descriptor, boolean visible) {
		if (0 == index.length) {
			return null;
		}
		if (!this.allSame(index)) {
			return null;
		}
		Annotation annotation = Annotation.builder(Type.getType(descriptor)).visible(visible).build();
		int[] key = { index[0], Math.max(0, this.labels.indexOf(start[0]) - 1), this.labels.indexOf(end[0]) };  // Revert the offset added above
		this.localAnnotations.computeIfAbsent(key, p -> new HashMap<>()).put(annotation.getType(), annotation);
		return new AnnotationScanner(annotation.getValues()::put);
	}
	
	@Override
	public void visitEnd() {
		Type[] types = this.method.getType().getArgumentTypes();
		for (int i = 0; i < types.length; i++) {
			Parameter.Builder builder = Parameter.builder(this.method).index(i).type(types[i]).annotations(this.parameterAnnotations.getOrDefault(i, new HashMap<>()));
			Map.Entry<String, Set<TypeModifier>> entry = this.parameters.getOrDefault(i, Map.entry("arg" + i, EnumSet.noneOf(TypeModifier.class)));
			this.method.getParameters().put(i, builder.name(entry.getKey()).modifiers(entry.getValue()).build());
		}
		for (Map.Entry<int[], Map<Type, Annotation>> entry : this.localAnnotations.entrySet()) {
			int[] key = entry.getKey();
			LocalVariable local = this.method.getLocal(key[0], key[1], key[2]);
			if (local != null) {
				local.getAnnotations().putAll(entry.getValue());
			}
		}
	}
	
	//region Helper methods
	private boolean allSame(int @NotNull [] index) {
		int first = index[0];
		for (int i = 1; i < index.length; i++) {
			if (first != index[i]) {
				return false;
			}
		}
		return true;
	}
	//endregion
}
