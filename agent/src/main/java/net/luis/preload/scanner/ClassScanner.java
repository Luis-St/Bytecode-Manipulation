package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.preload.data.*;
import net.luis.preload.data.type.TypeAccess;
import net.luis.preload.data.type.TypeModifier;
import org.objectweb.asm.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class ClassScanner extends BaseClassVisitor {
	
	private final List<AnnotationScanData> classAnnotations = new ArrayList<>();
	private final List<RecordComponentScanData> recordComponents = new ArrayList<>();
	private final List<FieldScanData> fields = new ArrayList<>();
	private final List<MethodScanData> methods = new ArrayList<>();
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		AnnotationScanData data = new AnnotationScanData(Type.getType(descriptor), values);
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
	
	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		//System.out.println();
		//System.out.println("Record Component: " + name);
		//System.out.println("  Type: " + Type.getType(descriptor));
		//System.out.println("  Signature: " + signature);
		List<AnnotationScanData> componentAnnotations = new ArrayList<>();
		this.recordComponents.add(new RecordComponentScanData(name, Type.getType(descriptor), signature, componentAnnotations));
		return new RecordComponentScanner(componentAnnotations::add);
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object initialValue) {
		//System.out.println();
		//System.out.println("Field: " + name);
		//System.out.println("  Type: " + Type.getType(descriptor));
		//System.out.println("  Access: " + TypeAccess.fromAccess(access));
		//System.out.println("  Modifiers: " + TypeModifier.fromFieldAccess(access));
		//System.out.println("  Signature: " + signature);
		//System.out.println("  Initial value: " + initialValue);
		List<AnnotationScanData> fieldAnnotations = new ArrayList<>();
		this.fields.add(new FieldScanData(name, Type.getType(descriptor), signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), fieldAnnotations, initialValue));
		return new FieldScanner(fieldAnnotations::add);
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
		List<AnnotationScanData> methodAnnotations = new ArrayList<>();
		List<ParameterScanData> methodParameters = new ArrayList<>();
		List<Type> methodExceptions = Optional.ofNullable(exceptions).stream().flatMap(Arrays::stream).map(iface -> "L" + iface + ";").map(Type::getType).collect(Collectors.toList());
		this.methods.add(new MethodScanData(name, Type.getType(descriptor), TypeAccess.fromAccess(access), TypeModifier.fromMethodAccess(access), methodAnnotations, methodParameters, methodExceptions));
		return new MethodScanner(methodAnnotations::add, methodParameters::add);
	}
}
