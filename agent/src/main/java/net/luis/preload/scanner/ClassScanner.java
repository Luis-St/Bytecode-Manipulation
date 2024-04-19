package net.luis.preload.scanner;

import net.luis.asm.base.visitor.*;
import net.luis.preload.data.*;
import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class ClassScanner extends BaseClassVisitor {
	
	private final List<AnnotationData> classAnnotations = new ArrayList<>();
	private final List<RecordComponentData> recordComponents = new ArrayList<>();
	private final List<FieldData> fields = new ArrayList<>();
	private final List<MethodData> methods = new ArrayList<>();
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		AnnotationData data = new AnnotationData(Type.getType(descriptor), values);
		this.classAnnotations.add(data);
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		//System.out.println();
		//System.out.println("Class: " + name);
		//System.out.println("  Type: " + ClassType.fromAccess(access));
		//System.out.println("  Access: " + TypeAccess.fromAccess(access));
		//System.out.println("  Modifiers: " + TypeModifier.fromClassAccess(access));
		//System.out.println("  Signature: " + signature);
		//System.out.println("  Super: " + Type.getType("L" + superName + ";"));
		//if (interfaces != null) {
		//	System.out.println("  Interfaces: " + Arrays.stream(interfaces).map(iface -> "L" + iface + ";").map(Type::getType).collect(Collectors.toList()));
		//}
	}
	
	private AnnotationVisitor createAnnotationScanner(String descriptor, Consumer<AnnotationData> action) {
		Map<String, Object> values = new HashMap<>();
		action.accept(new AnnotationData(Type.getType(descriptor), values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String recordDescriptor, String signature) {
		//System.out.println();
		//System.out.println("Record Component: " + name);
		//System.out.println("  Type: " + Type.getType(recordDescriptor));
		//System.out.println("  Signature: " + signature);
		List<AnnotationData> componentAnnotations = new ArrayList<>();
		this.recordComponents.add(new RecordComponentData(name, Type.getType(recordDescriptor), signature, componentAnnotations));
		return new BaseRecordComponentVisitor() {
			@Override
			public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
				return ClassScanner.this.createAnnotationScanner(annotationDescriptor, componentAnnotations::add);
			}
		};
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String fieldDescriptor, String signature, Object initialValue) {
		//System.out.println();
		//System.out.println("Field: " + name);
		//System.out.println("  Type: " + Type.getType(fieldDescriptor));
		//System.out.println("  Access: " + TypeAccess.fromAccess(access));
		//System.out.println("  Modifiers: " + TypeModifier.fromFieldAccess(access));
		//System.out.println("  Signature: " + signature);
		//System.out.println("  Initial value: " + initialValue);
		List<AnnotationData> fieldAnnotations = new ArrayList<>();
		this.fields.add(new FieldData(name, Type.getType(fieldDescriptor), signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), fieldAnnotations, initialValue));
		return new BaseFieldVisitor() {
			
			@Override
			public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
				return ClassScanner.this.createAnnotationScanner(annotationDescriptor, fieldAnnotations::add);
			}
		};
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		//System.out.println();
		//System.out.println("Method: " + name);
		//System.out.println("  Type: " + Type.getType(descriptor));
		//System.out.println("  Access: " + TypeAccess.fromAccess(access));
		//System.out.println("  Modifiers: " + TypeModifier.fromMethodAccess(access));
		//System.out.println("  Signature: " + signature);
		//if (exceptions != null) {
		//	System.out.println("  Exceptions: " + Arrays.stream(exceptions).map(iface -> "L" + iface + ";").map(Type::getType).collect(Collectors.toList()));
		//}
		List<AnnotationData> methodAnnotations = new ArrayList<>();
		List<ParameterScanData> methodParameters = new ArrayList<>();
		List<Type> methodExceptions = Optional.ofNullable(exceptions).stream().flatMap(Arrays::stream).map(iface -> "L" + iface + ";").map(Type::getType).collect(Collectors.toList());
		this.methods.add(new MethodData(name, Type.getType(descriptor), TypeAccess.fromAccess(access), TypeModifier.fromMethodAccess(access), methodAnnotations, methodParameters, methodExceptions));
		return new MethodScanner(methodAnnotations::add, methodParameters::add);
	}
}
