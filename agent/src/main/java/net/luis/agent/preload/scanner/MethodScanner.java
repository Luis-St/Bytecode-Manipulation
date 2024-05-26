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
	
	private final boolean isStatic;
	private final Type[] parameterTypes;
	private final BiConsumer<Type, AnnotationData> annotationConsumer;
	private final Consumer<ParameterData> parameterConsumer;
	private final BiConsumer<Integer, LocalVariableData> variableConsumer;
	private final Consumer<Object> annotationDefaultConsumer;
	private final Map<Integer, Map.Entry<String, Set<TypeModifier>>> parameters = new HashMap<>();
	private final Map<Integer, Map<Type, AnnotationData>> parameterAnnotations = new HashMap<>();
	private int parameterIndex = 0;
	
	public MethodScanner(boolean isStatic, Type @NotNull [] parameterTypes, @NotNull BiConsumer<Type, AnnotationData> annotationConsumer, @NotNull Consumer<ParameterData> parameterConsumer,
						 @NotNull BiConsumer<Integer, LocalVariableData> variableConsumer, @NotNull Consumer<Object> annotationDefaultConsumer) {
		super(() -> {});
		this.isStatic = isStatic;
		this.parameterTypes = parameterTypes;
		this.annotationConsumer = annotationConsumer;
		this.parameterConsumer = parameterConsumer;
		this.variableConsumer = variableConsumer;
		this.annotationDefaultConsumer = annotationDefaultConsumer;
	}
	
	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return new AnnotationScanner((name, value) -> this.annotationDefaultConsumer.accept(value));
	}
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		this.annotationConsumer.accept(type, new AnnotationData(type, visible, values));
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
		int offset = this.parameters.size() + (this.isStatic ? 0 : 1);
		if (index < offset) {
			return;
		}
		this.variableConsumer.accept(index, new LocalVariableData(index, name, Type.getType(descriptor), signature));
	}
	
	@Override
	public void visitEnd() {
		for (int i = this.parameterIndex; i < this.parameterTypes.length; i++) {
			Map.Entry<String, Set<TypeModifier>> entry = this.parameters.getOrDefault(i, Map.entry("arg" + i, EnumSet.noneOf(TypeModifier.class)));
			Map<Type, AnnotationData> annotations = this.parameterAnnotations.getOrDefault(i, new HashMap<>());
			this.parameterConsumer.accept(new ParameterData(entry.getKey(), this.parameterTypes[i], i, entry.getValue(), annotations));
		}
	}
}
