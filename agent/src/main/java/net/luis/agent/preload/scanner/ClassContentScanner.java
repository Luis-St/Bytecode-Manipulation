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
	
	public ClassContentScanner() {
		super(() -> {});
	}
	
	private @NotNull AnnotationVisitor createAnnotationScanner(@NotNull String descriptor, boolean visible, @NotNull BiConsumer<Type, AnnotationData> action) {
		Map<String, Object> values = new HashMap<>();
		Type type = Type.getType(descriptor);
		action.accept(type, new AnnotationData(type, visible, values));
		return new AnnotationScanner(values::put);
	}
	
	@Override
	public @NotNull RecordComponentVisitor visitRecordComponent(@NotNull String name, @NotNull String recordDescriptor, @Nullable String signature) {
		Map<Type, AnnotationData> annotations = new HashMap<>();
		this.recordComponents.put(name, new RecordComponentData(name, Type.getType(recordDescriptor), signature, annotations));
		return new BaseRecordComponentVisitor() {
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return ClassContentScanner.this.createAnnotationScanner(annotationDescriptor, visible, annotations::put);
			}
		};
	}
	
	@Override
	public @NotNull FieldVisitor visitField(int access, @NotNull String name, @NotNull String fieldDescriptor, @Nullable String signature, @Nullable Object initialValue) {
		Map<Type, AnnotationData> annotations = new HashMap<>();
		this.fields.put(name, new FieldData(name, Type.getType(fieldDescriptor), signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), annotations, initialValue));
		return new BaseFieldVisitor() {
			
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return ClassContentScanner.this.createAnnotationScanner(annotationDescriptor, visible, annotations::put);
			}
		};
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exception) {
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
