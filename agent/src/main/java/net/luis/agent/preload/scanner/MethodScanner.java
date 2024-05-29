package net.luis.agent.preload.scanner;

import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author Luis-St
 *
 */

public class MethodScanner extends BaseMethodVisitor {
	
	private final MethodData method;
	private final Map<Integer, Map.Entry<String, Set<TypeModifier>>> parameters = new HashMap<>();
	private final Map<Integer, Map<Type, AnnotationData>> parameterAnnotations = new HashMap<>();
	private int parameterIndex = 0;
	
	public MethodScanner(@NotNull MethodData method) {
		super(() -> {});
		this.method = method;
	}
	
	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return new AnnotationScanner((name, value) -> this.method.annotationDefault().accept(value));
	}
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		this.method.annotations().put(type, new AnnotationData(type, visible, values));
		return new AnnotationScanner(values::put);
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
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		this.parameterAnnotations.computeIfAbsent(parameter, p -> new HashMap<>()).put(type, new AnnotationData(type, visible, values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visitLocalVariable(@NotNull String name, @NotNull String descriptor, @Nullable String signature, @NotNull Label start, @NotNull Label end, int index) {
		int offset = this.parameters.size() + (this.method.is(TypeModifier.STATIC) ? 0 : 1);
		if (index < offset) {
			return;
		}
		this.method.localVariables().put(index, new LocalVariableData(this.method, index, name, Type.getType(descriptor), signature));
	}
	
	@Override
	public void visitEnd() {
		Type[] types = this.method.type().getArgumentTypes();
		for (int i = 0; i < types.length; i++) {
			Map.Entry<String, Set<TypeModifier>> entry = this.parameters.getOrDefault(i, Map.entry("arg" + i, EnumSet.noneOf(TypeModifier.class)));
			Map<Type, AnnotationData> annotations = this.parameterAnnotations.getOrDefault(i, new HashMap<>());
			this.method.parameters().add(new ParameterData(this.method, entry.getKey(), types[i], i, entry.getValue(), annotations));
		}
	}
}
