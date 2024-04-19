package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseMethodVisitor;
import net.luis.preload.data.*;
import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Consumer;

/**
 *
 * @author Luis-St
 *
 */

public class MethodScanner extends BaseMethodVisitor {
	
	private final Consumer<AnnotationData> annotationConsumer;
	private final Consumer<ParameterData> parameterConsumer;
	private final List<Map.Entry<String, List<TypeModifier>>> parameters = new ArrayList<>();
	private final Map<Integer, List<AnnotationData>> parameterAnnotations = new HashMap<>();
	private int parameterIndex = 0;
	
	public MethodScanner(Consumer<AnnotationData> annotationConsumer, Consumer<ParameterData> parameterConsumer) {
		this.annotationConsumer = annotationConsumer;
		this.parameterConsumer = parameterConsumer;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		this.annotationConsumer.accept(new AnnotationData(Type.getType(descriptor), values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visitParameter(String name, int access) {
		if (name == null) {
			name = "arg" + this.parameterIndex;
		}
		this.parameterIndex++;
		//System.out.println("Parameter name: " + name);
		//System.out.println("  Modifier: " + TypeModifier.fromParameterAccess(access));
		this.parameters.add(Map.entry(name, TypeModifier.fromParameterAccess(access)));
	}
	
	@Override
	public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
		//System.out.println("Parameter index: " + parameter);
		//System.out.println("  Type: " + Type.getType(descriptor));
		Map<String, Object> values = new HashMap<>();
		this.parameterAnnotations.computeIfAbsent(parameter, p -> new ArrayList<>()).add(new AnnotationData(Type.getType(descriptor), values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visitEnd() {
		for (int i = 0; i < this.parameters.size(); i++) {
			Map.Entry<String, List<TypeModifier>> entry =  this.parameters.get(i);
			List<AnnotationData> annotations = this.parameterAnnotations.getOrDefault(i, Collections.emptyList());
			this.parameterConsumer.accept(new ParameterData(entry.getKey(), i, entry.getValue(), annotations));
		}
	}
}
