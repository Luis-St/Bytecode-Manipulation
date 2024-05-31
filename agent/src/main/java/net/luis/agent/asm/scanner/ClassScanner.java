package net.luis.agent.asm.scanner;

import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.BiConsumer;

/**
 *
 * @author Luis-St
 *
 */

public class ClassScanner extends ClassVisitor {
	
	private final Map<Type, Annotation> annotations = new HashMap<>();
	private final Set<TypeModifier> modifiers = EnumSet.noneOf(TypeModifier.class);
	private final List<Type> permittedSubclasses = new ArrayList<>();
	private final List<Type> interfaces = new ArrayList<>();
	private final Map<String, RecordComponent> recordComponents = new HashMap<>();
	private final Map<String, Field> fields = new HashMap<>();
	private final Map<String, Method> methods = new HashMap<>();
	private final List<InnerClass> innerClasses = new ArrayList<>();
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
	private static @NotNull AnnotationVisitor createAnnotationScanner(@NotNull String descriptor, boolean visible, @NotNull BiConsumer<Type, Annotation> action) {
		Annotation annotation = Annotation.builder(Type.getType(descriptor)).visible(visible).build();
		action.accept(annotation.getType(), annotation);
		return new AnnotationScanner(annotation.getValues()::put);
	}
	//endregion
	
	@Override
	public @NotNull AnnotationVisitor visitAnnotation(@NotNull String descriptor, boolean visible) {
		Annotation annotation = Annotation.builder(Type.getType(descriptor)).visible(visible).build();
		this.annotations.put(annotation.getType(), annotation);
		return new AnnotationScanner(annotation.getValues()::put);
	}
	
	@Override
	public void visit(int version, int access, @NotNull String name, @Nullable String genericSignature, @Nullable String superClass, String @Nullable [] interfaces) {
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
		this.signature = genericSignature;
		this.modifiers.addAll(TypeModifier.fromClassAccess(access));
		this.superType = Type.getObjectType(superClass);
		this.interfaces.addAll(Arrays.stream(interfaces).map(Type::getObjectType).toList());
	}
	
	@Override
	public void visitPermittedSubclass(@NotNull String permittedSubclass) {
		this.permittedSubclasses.add(Type.getObjectType(permittedSubclass));
	}
	
	@Override
	public @NotNull RecordComponentVisitor visitRecordComponent(@NotNull String name, @NotNull String recordDescriptor, @Nullable String genericSignature) {
		RecordComponent recordComponent = RecordComponent.builder(this.type, name, Type.getType(recordDescriptor)).genericSignature(genericSignature).build();
		this.recordComponents.put(name, recordComponent);
		return new RecordComponentVisitor(Opcodes.ASM9) {
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return createAnnotationScanner(annotationDescriptor, visible, recordComponent.getAnnotations()::put);
			}
		};
	}
	
	@Override
	public @NotNull FieldVisitor visitField(int access, @NotNull String name, @NotNull String fieldDescriptor, @Nullable String genericSignature, @Nullable Object initialValue) {
		Field field = Field.of(this.type, name, Type.getType(fieldDescriptor), genericSignature, access);
		field.getInitialValue().set(initialValue);
		this.fields.put(name, field);
		return new FieldVisitor(Opcodes.ASM9) {
			
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return createAnnotationScanner(annotationDescriptor, visible, field.getAnnotations()::put);
			}
		};
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String genericSignature, String @Nullable [] exception) {
		Method method = Method.of(this.type, name, Type.getType(descriptor), genericSignature, access);
		method.getExceptions().addAll(Optional.ofNullable(exception).stream().flatMap(Arrays::stream).map(Type::getObjectType).toList());
		this.methods.put(method.getFullSignature(), method);
		return new MethodScanner(method);
	}
	
	@Override
	public void visitInnerClass(@NotNull String name, @Nullable String outerName, @Nullable String innerName, int access) {
		this.innerClasses.add(InnerClass.of(this.type, innerName, Type.getObjectType(name), outerName, innerName, access));
	}
	
	public @NotNull Class get() {
		return Class.builder(this.name, this.type, this.classType).genericSignature(this.signature).access(this.access).modifiers(this.modifiers).superType(this.superType).permittedSubclasses(this.permittedSubclasses)
			.interfaces(this.interfaces).annotations(this.annotations).recordComponents(this.recordComponents).fields(this.fields).methods(this.methods).innerClasses(this.innerClasses).build();
	}
}
