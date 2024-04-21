package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.preload.data.AnnotationData;
import net.luis.preload.data.ClassInfo;
import net.luis.preload.type.*;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public class ClassInfoScanner extends BaseClassVisitor {
	
	private final List<AnnotationData> classAnnotations = new ArrayList<>();
	private final List<TypeModifier> modifiers = new ArrayList<>();
	private final List<Type> interfaces = new ArrayList<>();
	private String name;
	private Type type;
	private String signature;
	private TypeAccess access;
	private ClassType classType;
	private Type superType;
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		AnnotationData data = new AnnotationData(Type.getType(descriptor), values);
		this.classAnnotations.add(data);
		return new AnnotationScanner(values::put);
	}
	
	@Override
		System.out.println();
		System.out.println("Class: " + name);
		System.out.println("  Type: " + ClassType.fromAccess(access));
		System.out.println("  Access: " + TypeAccess.fromAccess(access));
		System.out.println("  Modifiers: " + TypeModifier.fromClassAccess(access));
		System.out.println("  Signature: " + signature);
		if (superName != null) {
			System.out.println("  Super: " + Type.getObjectType(superName));
	public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
		}
		if (interfaces != null) {
			System.out.println("  Interfaces: " + Arrays.stream(interfaces).map(Type::getObjectType).toList());
		}
		int index = name.lastIndexOf('/');
		this.name = index == -1 ? name : name.substring(index + 1);
		this.type = Type.getObjectType(name);
		this.signature = signature;
		this.access = TypeAccess.fromAccess(access);
		this.classType = ClassType.fromAccess(access);
		this.modifiers.addAll(TypeModifier.fromClassAccess(access));
		if (superName != null) {
			this.superType = Type.getObjectType(superName);
		}
		if (interfaces != null) {
			this.interfaces.addAll(Arrays.stream(interfaces).map(Type::getObjectType).toList());
		}
	}
	
	public @NotNull ClassInfo getClassInfo() {
		return new ClassInfo(this.name, this.type, this.signature, this.access, this.classType, this.modifiers, this.superType, this.interfaces, this.classAnnotations);
	}
}
