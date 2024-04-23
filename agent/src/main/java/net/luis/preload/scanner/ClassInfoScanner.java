package net.luis.preload.scanner;

import net.luis.asm.base.visitor.BaseClassVisitor;
import net.luis.preload.data.AnnotationData;
import net.luis.preload.data.ClassInfo;
import net.luis.preload.type.*;
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
	
	private final Map<Type, AnnotationData> classAnnotations = new HashMap<>();
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
		/*System.out.println("Annotation: " + descriptor);*/
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		AnnotationData data = new AnnotationData(type, values);
		this.classAnnotations.put(type, data);
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public ModuleVisitor visitModule(String name, int access, String version) {
		/*System.out.println();
		System.out.println("Module: " + name);*/
		this.name = name;
		this.type = Type.getType("Lmodule-info;");
		this.access = TypeAccess.PUBLIC;
		this.classType = ClassType.MODULE;
		return super.visitModule(name, access, version);
	}
	
	@Override
	public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
		ClassType type = ClassType.fromAccess(access);
		if (this.classType == ClassType.MODULE || type == ClassType.MODULE) {
			return;
		}
		Objects.requireNonNull(superClass, "Super class is null");
		Objects.requireNonNull(interfaces, "Interfaces are null");
		int index = name.lastIndexOf('/');
		this.name = index == -1 ? name : name.substring(index + 1);
		this.type = Type.getObjectType(name);
		this.access = TypeAccess.fromAccess(access);
		this.classType = ClassType.fromAccess(access);
		this.signature = signature;
		this.modifiers.addAll(TypeModifier.fromClassAccess(access));
		this.superType = Type.getObjectType(superClass);
		this.interfaces.addAll(Arrays.stream(interfaces).map(Type::getObjectType).toList());
		/*System.out.println();
		System.out.println("Class: " + this.name);
		System.out.println("  Type: " + this.type);
		System.out.println("  Class type: " + this.classType);
		System.out.println("  Access: " + this.access);
		System.out.println("  Modifiers: " + this.modifiers);
		System.out.println("  Signature: " + this.signature);
		System.out.println("  Super: " + this.superType);
		System.out.println("  Interfaces: " + this.interfaces);*/
	}
	
	public @NotNull ClassInfo getClassInfo() {
		return new ClassInfo(this.name, this.type, this.signature, this.access, this.classType, this.modifiers, this.superType, this.interfaces, this.classAnnotations);
	}
}
