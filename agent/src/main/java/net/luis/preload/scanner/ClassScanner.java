package net.luis.preload.scanner;

import net.luis.asm.ASMHelper;
import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.asm.base.visitor.BaseRecordComponentVisitor;
import net.luis.preload.data.*;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class ClassScanner extends BaseClassVisitor {
	
	private final List<AnnotationScanData> classAnnotations = ASMHelper.newList();
	private final List<RecordComponentScanData> recordComponents = ASMHelper.newList();
	private final List<FieldScanData> fields = ASMHelper.newList();
	
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
		//System.out.println("  Interfaces: " + Arrays.stream(interfaces).map(iface -> "L" + iface + ";").map(Type::getType).toList());
	}
	
	@Override
	public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
		//System.out.println();
		//System.out.println("Record Component: " + name);
		//System.out.println("  Type: " + Type.getType(descriptor));
		//System.out.println("  Signature: " + signature);
		List<AnnotationScanData> componentAnnotations = ASMHelper.newList();
		this.recordComponents.add(new RecordComponentScanData(Type.getType(descriptor), name, signature, componentAnnotations));
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
		List<AnnotationScanData> fieldAnnotations = ASMHelper.newList();
		this.fields.add(new FieldScanData(Type.getType(descriptor), name, signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), fieldAnnotations, initialValue));
		return new FieldScanner(fieldAnnotations::add);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodScanner();
	}
}
