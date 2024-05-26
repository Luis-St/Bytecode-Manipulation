package net.luis.agent.preload.scanner;

import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.*;
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
	
	private final Map<Type, AnnotationData> annotations = new HashMap<>();
	private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
	private final List<Type> permittedSubclasses = new ArrayList<>();
	private final List<Type> interfaces = new ArrayList<>();
	private final List<InnerClassData> innerClasses = new ArrayList<>();
	private String name;
	private Type type;
	private String signature;
	private TypeAccess access;
	private ClassType classType;
	private Type superType;
	
	public ClassInfoScanner() {
		super(() -> {});
	}
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		AnnotationData data = new AnnotationData(type, visible, values);
		this.annotations.put(type, data);
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
		ClassType type = ClassType.fromAccess(access);
		if (type == ClassType.MODULE) {
			return;
		}
		Objects.requireNonNull(superClass, "Super class is null");
		Objects.requireNonNull(interfaces, "Interfaces are null");
		int index = name.lastIndexOf('/');
		this.name = index == -1 ? name : name.substring(index + 1);
		this.type = Type.getObjectType(name);
		this.access = TypeAccess.fromAccess(access);
		this.classType = type;
		this.signature = signature;
		this.modifiers.addAll(TypeModifier.fromClassAccess(access));
		this.superType = Type.getObjectType(superClass);
		this.interfaces.addAll(Arrays.stream(interfaces).map(Type::getObjectType).toList());
	}
	
	@Override
	public void visitPermittedSubclass(@NotNull String permittedSubclass) {
		this.permittedSubclasses.add(Type.getObjectType(permittedSubclass));
	}
	
	@Override
	public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
		InnerClassType classType = InnerClassType.fromNames(outerName, innerName);
		TypeAccess typeAccess = classType == InnerClassType.INNER ? TypeAccess.fromAccess(access) : TypeAccess.PRIVATE;
		Set<TypeModifier> modifiers = classType == InnerClassType.INNER ? TypeModifier.fromClassAccess(access) : EnumSet.noneOf(TypeModifier.class);
		this.innerClasses.add(new InnerClassData(innerName, Type.getObjectType(name), typeAccess, classType, modifiers));
	}
	
	public @NotNull ClassInfo getClassInfo() {
		return new ClassInfo(this.name, this.type, this.signature, this.access, this.classType, this.modifiers, this.superType, this.interfaces, this.permittedSubclasses, this.annotations, this.innerClasses);
	}
}
