package net.luis.agent.preload.scanner;

import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.*;
import net.luis.agent.util.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class ClassScanner extends ClassVisitor {
	
	private final Map<Type, AnnotationData> annotations = new HashMap<>();
	private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
	private final List<Type> permittedSubclasses = new ArrayList<>();
	private final List<Type> interfaces = new ArrayList<>();
	private final Map<String, RecordComponentData> recordComponents = new HashMap<>();
	private final Map<String, FieldData> fields = new HashMap<>();
	private final List<MethodData> methods = new ArrayList<>();
	private final List<InnerClassData> innerClasses = new ArrayList<>();
	private String name;
	private Type type;
	private String signature;
	private TypeAccess access;
	private ClassType classType;
	private Type superType;
	
	public ClassScanner() {
		super(Opcodes.ASM9);
	}
	
	//region Static helper methods
	private static @NotNull AnnotationVisitor createAnnotationScanner(@NotNull String descriptor, boolean visible, @NotNull BiConsumer<Type, AnnotationData> action) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		action.accept(type, new AnnotationData(type, visible, values));
		return new AnnotationScanner(values::put);
	}
	//endregion
	
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
	public @NotNull RecordComponentVisitor visitRecordComponent(@NotNull String name, @NotNull String recordDescriptor, @Nullable String signature) {
		Map<Type, AnnotationData> annotations = new HashMap<>();
		this.recordComponents.put(name, new RecordComponentData(this.type, name, Type.getType(recordDescriptor), signature, annotations));
		return new RecordComponentVisitor(Opcodes.ASM9) {
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return createAnnotationScanner(annotationDescriptor, visible, annotations::put);
			}
		};
	}
	
	@Override
	public @NotNull FieldVisitor visitField(int access, @NotNull String name, @NotNull String fieldDescriptor, @Nullable String signature, @Nullable Object initialValue) {
		Map<Type, AnnotationData> annotations = new HashMap<>();
		this.fields.put(name, new FieldData(this.type, name, Type.getType(fieldDescriptor), signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), annotations, initialValue));
		return new FieldVisitor(Opcodes.ASM9) {
			
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return createAnnotationScanner(annotationDescriptor, visible, annotations::put);
			}
		};
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exception) {
		List<Type> exceptions = Optional.ofNullable(exception).stream().flatMap(Arrays::stream).map(Type::getObjectType).collect(Collectors.toList());
		MethodData method = new MethodData(this.type, name, Type.getType(descriptor), signature, TypeAccess.fromAccess(access), MethodType.fromName(name), TypeModifier.fromMethodAccess(access), new HashMap<>(), new ArrayList<>(), exceptions, new HashMap<>(), new Mutable<>());
		this.methods.add(method);
		return new MethodScanner(method);
	}
	
	@Override
	public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
		InnerClassType classType = InnerClassType.fromNames(outerName, innerName);
		TypeAccess typeAccess = classType == InnerClassType.INNER ? TypeAccess.fromAccess(access) : TypeAccess.PRIVATE;
		Set<TypeModifier> modifiers = classType == InnerClassType.INNER ? TypeModifier.fromClassAccess(access) : EnumSet.noneOf(TypeModifier.class);
		this.innerClasses.add(new InnerClassData(this.type, innerName, Type.getObjectType(name), typeAccess, classType, modifiers));
	}
	
	public @NotNull ClassData getClassData() {
		return new ClassData(this.name, this.type, this.signature, this.access, this.classType, this.modifiers, this.superType, this.permittedSubclasses, this.interfaces, this.annotations,
			this.recordComponents, this.fields, this.methods, this.innerClasses);
	}
}
