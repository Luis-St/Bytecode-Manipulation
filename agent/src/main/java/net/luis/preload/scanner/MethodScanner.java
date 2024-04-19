package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseMethodVisitor;
import net.luis.preload.data.*;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Consumer;

/**
 *
 * @author Luis-St
 *
 */

public class MethodScanner extends BaseMethodVisitor {
	
	private final Consumer<AnnotationScanData> annotationConsumer;
	private final Consumer<ParameterScanData> parameterConsumer;
	private final List<Map.Entry<String, List<TypeModifier>>> parameters = new ArrayList<>();
	private final Map<Integer, List<AnnotationScanData>> parameterAnnotations = new HashMap<>();
	private int parameterIndex = 0;
	
	public MethodScanner(Consumer<AnnotationScanData> annotationConsumer, Consumer<ParameterScanData> parameterConsumer) {
		this.annotationConsumer = annotationConsumer;
		this.parameterConsumer = parameterConsumer;
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		this.annotationConsumer.accept(new AnnotationScanData(Type.getType(descriptor), values));
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
		this.parameterAnnotations.computeIfAbsent(parameter, p -> new ArrayList<>()).add(new AnnotationScanData(Type.getType(descriptor), values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visitEnd() {
		for (int i = 0; i < this.parameters.size(); i++) {
			Map.Entry<String, List<TypeModifier>> entry =  this.parameters.get(i);
			List<AnnotationScanData> annotations = this.parameterAnnotations.getOrDefault(i, Collections.emptyList());
			this.parameterConsumer.accept(new ParameterScanData(entry.getKey(), i, entry.getValue(), annotations));
		}
	}
}
