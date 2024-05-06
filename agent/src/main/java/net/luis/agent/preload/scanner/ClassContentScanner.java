package net.luis.agent.preload.scanner;

import net.luis.agent.asm.base.visitor.*;
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

public class ClassContentScanner extends BaseClassVisitor {
	
	private final Map<String, RecordComponentData> recordComponents = new HashMap<>();
	private final Map<String, FieldData> fields = new HashMap<>();
	private final List<MethodData> methods = new ArrayList<>();
	
	private @NotNull AnnotationVisitor createAnnotationScanner(@NotNull String descriptor, @NotNull BiConsumer<Type, AnnotationData> action) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		action.accept(type, new AnnotationData(type, values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public @NotNull RecordComponentVisitor visitRecordComponent(@NotNull String name, @NotNull String recordDescriptor, @Nullable String signature) {
		/*System.out.println();
		System.out.println("Record Component: " + name);
		System.out.println("  Type: " + Type.getType(recordDescriptor));
		System.out.println("  Signature: " + signature);*/
		Map<Type, AnnotationData> annotations = new HashMap<>();
		this.recordComponents.put(name, new RecordComponentData(name, Type.getType(recordDescriptor), signature, annotations));
		return new BaseRecordComponentVisitor() {
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return ClassContentScanner.this.createAnnotationScanner(annotationDescriptor, annotations::put);
			}
		};
	}
	
	@Override
	public @NotNull FieldVisitor visitField(int access, @NotNull String name, @NotNull String fieldDescriptor, @Nullable String signature, @Nullable Object initialValue) {
		/*System.out.println();
		System.out.println("Field: " + name);
		System.out.println("  Type: " + Type.getType(fieldDescriptor));
		System.out.println("  Access: " + TypeAccess.fromAccess(access));
		System.out.println("  Modifiers: " + TypeModifier.fromFieldAccess(access));
		System.out.println("  Signature: " + signature);
		System.out.println("  Initial value: " + initialValue);*/
		Map<Type, AnnotationData> annotations = new HashMap<>();
		this.fields.put(name, new FieldData(name, Type.getType(fieldDescriptor), signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), annotations, initialValue));
		return new BaseFieldVisitor() {
			
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return ClassContentScanner.this.createAnnotationScanner(annotationDescriptor, annotations::put);
			}
		};
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exception) {
		/*System.out.println();
		System.out.println("Method: " + name);
		System.out.println("  Type: " + Type.getType(descriptor));
		System.out.println("  Parameters: " + Arrays.toString(Type.getArgumentTypes(descriptor)));
		System.out.println("  Access: " + TypeAccess.fromAccess(access));
		System.out.println("  Modifiers: " + TypeModifier.fromMethodAccess(access));
		System.out.println("  Signature: " + signature);
		if (exceptions != null) {
			System.out.println("  Exceptions: " + Arrays.stream(exceptions).map(Type::getObjectType).toList());
		}*/
		Map<Type, AnnotationData> annotations = new HashMap<>();
		List<ParameterData> parameters = new ArrayList<>();
		List<Type> exceptions = Optional.ofNullable(exception).stream().flatMap(Arrays::stream).map(Type::getObjectType).collect(Collectors.toList());
		Mutable<Object> value = new Mutable<>();
		this.methods.add(new MethodData(name, Type.getType(descriptor), signature, TypeAccess.fromAccess(access), MethodType.fromName(name), TypeModifier.fromMethodAccess(access), annotations, parameters, exceptions, value));
		return new MethodScanner(Type.getArgumentTypes(descriptor), annotations::put, parameters::add, value);
	}
	
	public @NotNull ClassContent getClassContent() {
		return new ClassContent(this.recordComponents, this.fields, this.methods);
	}
}
