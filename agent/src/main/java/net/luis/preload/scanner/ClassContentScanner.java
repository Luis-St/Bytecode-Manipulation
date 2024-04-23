package net.luis.preload.scanner;

import net.luis.asm.base.visitor.*;
import net.luis.preload.data.*;
import net.luis.preload.type.TypeAccess;
import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 * @author Luis-St
 *
 */

public class ClassContentScanner extends BaseClassVisitor {
	
	private final List<RecordComponentData> recordComponents = new ArrayList<>();
	private final List<FieldData> fields = new ArrayList<>();
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
		Map<Type, AnnotationData> componentAnnotations = new HashMap<>();
		this.recordComponents.add(new RecordComponentData(name, Type.getType(recordDescriptor), signature == null ? "" : signature, componentAnnotations));
		return new BaseRecordComponentVisitor() {
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return ClassContentScanner.this.createAnnotationScanner(annotationDescriptor, componentAnnotations::put);
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
		Map<Type, AnnotationData> fieldAnnotations = new HashMap<>();
		this.fields.add(new FieldData(name, Type.getType(fieldDescriptor), signature == null ? "" : signature, TypeAccess.fromAccess(access), TypeModifier.fromFieldAccess(access), fieldAnnotations, initialValue));
		return new BaseFieldVisitor() {
			
			@Override
			public AnnotationVisitor visitAnnotation(@NotNull String annotationDescriptor, boolean visible) {
				return ClassContentScanner.this.createAnnotationScanner(annotationDescriptor, fieldAnnotations::put);
			}
		};
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
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
		Map<Type, AnnotationData> methodAnnotations = new HashMap<>();
		List<ParameterData> methodParameters = new ArrayList<>();
		List<Type> methodExceptions = Optional.ofNullable(exceptions).stream().flatMap(Arrays::stream).map(Type::getObjectType).collect(Collectors.toList());
		this.methods.add(new MethodData(name, Type.getType(descriptor), signature == null ? "" : signature, TypeAccess.fromAccess(access), TypeModifier.fromMethodAccess(access), methodAnnotations, methodParameters, methodExceptions));
		return new MethodScanner(Type.getArgumentTypes(descriptor), methodAnnotations::put, methodParameters::add);
	}
	
	public @NotNull ClassContent getClassContent() {
		return new ClassContent(this.recordComponents, this.fields, this.methods);
	}
}
