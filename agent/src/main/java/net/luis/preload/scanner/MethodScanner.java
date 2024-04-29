package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseMethodVisitor;
import net.luis.preload.data.AnnotationData;
import net.luis.preload.data.ParameterData;
import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author Luis-St
 *
 */

public class MethodScanner extends BaseMethodVisitor {
	
	private final Type[] parameterTypes;
	private final BiConsumer<Type, AnnotationData> annotationConsumer;
	private final Consumer<ParameterData> parameterConsumer;
	private final List<Map.Entry<String, Set<TypeModifier>>> parameters = new ArrayList<>();
	private final Map<Integer, Map<Type, AnnotationData>> parameterAnnotations = new HashMap<>();
	private int parameterIndex = 0;
	
	public MethodScanner(Type @NotNull [] parameterTypes, @NotNull BiConsumer<Type, AnnotationData> annotationConsumer, @NotNull Consumer<ParameterData> parameterConsumer) {
		this.parameterTypes = parameterTypes;
		this.annotationConsumer = annotationConsumer;
		this.parameterConsumer = parameterConsumer;
	}
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		this.annotationConsumer.accept(type, new AnnotationData(type, values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visitParameter(@Nullable String name, int access) {
		if (name == null) {
			name = "arg" + this.parameterIndex;
		}
		this.parameterIndex++;
		/*System.out.println("Parameter name: " + name);
		System.out.println("  Modifier: " + TypeModifier.fromParameterAccess(access));*/
		this.parameters.add(Map.entry(name, TypeModifier.fromParameterAccess(access)));
	}
	
	@Override
	public @NotNull AnnotationVisitor visitParameterAnnotation(int parameter, @NotNull String descriptor, boolean visible) {
		/*System.out.println("Parameter index: " + parameter);
		System.out.println("  Type: " + Type.getType(descriptor));*/
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		this.parameterAnnotations.computeIfAbsent(parameter, p -> new HashMap<>()).put(type, new AnnotationData(type, values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visitEnd() {
		for (int i = 0; i < this.parameters.size(); i++) {
			Map.Entry<String, Set<TypeModifier>> entry =  this.parameters.get(i);
			Map<Type, AnnotationData> annotations = this.parameterAnnotations.getOrDefault(i, new HashMap<>());
			this.parameterConsumer.accept(new ParameterData(entry.getKey(), this.parameterTypes[i],  i, entry.getValue(), annotations));
		}
	}
}
