package net.luis.agent.asm.transformer.method;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Field;
import net.luis.agent.asm.data.Method;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;
import java.util.function.BiConsumer;

import static net.luis.agent.asm.Types.*;
import static net.luis.agent.asm.Instrumentations.*;

/**
 *
 * @author Luis-St
 *
 */

public class LazyTransformer extends BaseClassTransformer {
	
	private static final Type SUPPLIER = Type.getType("Ljava/util/function/Supplier;");
	private static final String REPORT_CATEGORY = "Invalid Annotated Element";
	
	private final Map</*Owner Class*/String, Set</*Field Name*/String>> lookup = createLazyLookup();
	
	//region Lookup creation
	private static @NotNull Map</*Owner Class*/String, Set</*Field Name*/String>> createLazyLookup() {
		Map<String, Set<String>> lookup = new HashMap<>();
		Agent.stream().flatMap(clazz -> clazz.getFields().values().stream()).filter(field -> field.isAnnotatedWith(LAZY)).forEach(field -> {
			lookup.computeIfAbsent(field.getOwner().getInternalName(), key -> new HashSet<>()).add(field.getName());
		});
		return lookup;
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new LazyClassVisitor(writer, type, this.lookup, () -> this.modified = true);
	}
	
	private static class LazyClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Owner Class*/String, Set</*Field Name*/String>> lookup;
		private final Map</*Name*/String, /*Factory Type*/Type> factories = new HashMap<>();
		private final Map</*Field Name*/String, /*Initial Value*/Object> converts = new HashMap<>();
		private final Set</*Field Name*/String> currentFields;
		
		private LazyClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Map</*Owner Class*/String, Set</*Field Name*/String>> lookup, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			this.lookup = lookup;
			this.currentFields = new HashSet<>(lookup.getOrDefault(type.getInternalName(), Collections.emptySet()));
		}
		
		@Override
		public @NotNull FieldVisitor visitField(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, @Nullable Object value) {
			if (this.currentFields.contains(name)) {
				//region Validation
				Field field = Agent.getClass(this.type).getField(name);
				if (field == null) {
					return super.visitField(access, name, descriptor, signature, value);
				}
				if (!field.is(TypeModifier.FINAL)) {
					throw CrashReport.create("Field annotated with @Lazy must be final", REPORT_CATEGORY).addDetail("Field", field.getSignature(SignatureType.DEBUG)).exception();
				}
				//endregion
				Type type = Type.getType(descriptor);
				if (isPrimitive(type) && is(access, Opcodes.ACC_STATIC)) {
					this.converts.put(name, value);
				}
				this.markModified();
				return super.visitField(access, name, SUPPLIER.getDescriptor(), "Ljava/util/function/Supplier<" + convertToWrapper(type).getDescriptor() + ">;", null);
			}
			return super.visitField(access, name, descriptor, signature, value);
		}
		
		private @NotNull LocalVariablesSorter createLocalSorter(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions)  {
			return new LocalVariablesSorter(access, descriptor, super.visitMethod(access, name, descriptor, signature, exceptions));
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			Method method = Agent.getClass(this.type).getMethod(name + descriptor);
			if (method == null) {
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
			LocalVariablesSorter sorter = this.createLocalSorter(access, name, descriptor, signature, exceptions);
			if (method.is(MethodType.STATIC_INITIALIZER)) {
				return new StaticInitializerMethodVisitor(sorter, method, this.lookup, this.converts, this.factories::put);
			}
			return new LazyMethodVisitor(sorter, method, this.lookup, this.factories::put);
		}
		
		@Override
		public void visitEnd() {
			if (!this.converts.isEmpty()) {
				//region Data update
				Method method = Method.builder(this.type, "<clinit>", VOID_METHOD).addModifier(TypeModifier.STATIC).build();
				Agent.getClass(this.type).getMethods().put(method.getSignature(SignatureType.FULL), method);
				//endregion
				MethodVisitor mv = this.visitMethod(Opcodes.ACC_STATIC, "<clinit>", VOID_METHOD.getDescriptor(), null, null);
				mv.visitCode();
				mv.visitInsn(Opcodes.RETURN);
				mv.visitMaxs(0, 0);
				mv.visitEnd();
			}
			for (Map.Entry<String, Type> entry : this.factories.entrySet()) {
				//region Data update
				String name = entry.getKey();
				Type type = entry.getValue();
				
				Method method = Method.builder(this.type, name, type).access(TypeAccess.PRIVATE).addModifier(TypeModifier.STATIC).addModifier(TypeModifier.SYNTHETIC).build();
				Agent.getClass(this.type).getMethods().put(method.getSignature(SignatureType.FULL), method);
				//endregion
				MethodVisitor visitor = this.cv.visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, type.getDescriptor(), null, null);
				visitor.visitCode();
				visitor.visitVarInsn(Opcodes.ALOAD, 0);
				visitor.visitInsn(Opcodes.ARETURN);
				visitor.visitMaxs(0, 0);
				visitor.visitEnd();
			}
			//region Data update
			for (String name : this.currentFields) {
				Field current = Agent.getClass(this.type).getField(name);
				if (current == null) {
					continue;
				}
				Field field = Field.builder(current).type(SUPPLIER).genericSignature("Ljava/util/function/Supplier<" + convertToWrapper(current.getType()).getDescriptor() + ">;").initialValue(null).build();
				Agent.getClass(this.type).getFields().replace(name, current, field);
			}
			//endregion
			super.visitEnd();
		}
	}
	
	private static class LazyMethodVisitor extends LabelTrackingMethodVisitor {
		
		private final Map</*Owner Class*/String, Set</*Field Name*/String>> lookup;
		private final Set<String> modified = new HashSet<>();
		private final BiConsumer<String, Type> factory;
		protected final Method method;
		
		private LazyMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method, @NotNull Map<String, Set<String>> lookup, @NotNull BiConsumer<String, Type> factory) {
			super(visitor);
			this.setMethod(method);
			this.method = method;
			this.lookup = lookup;
			this.factory = factory;
		}
		
		@Override
		public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor) {
			Type original = Type.getType(descriptor);
			if (this.lookup.containsKey(owner) && this.lookup.get(owner).contains(name)) {
				if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
					super.visitFieldInsn(opcode, owner, name, SUPPLIER.getDescriptor());
					this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, SUPPLIER.getInternalName(), "get", "()Ljava/lang/Object;", true);
					if (isPrimitive(original)) {
						Type wrapper = convertToWrapper(original);
						this.mv.visitTypeInsn(Opcodes.CHECKCAST, wrapper.getInternalName());
						this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper.getInternalName(), original.getClassName() + "Value", "()" + original.getDescriptor(), false);
					} else {
						this.mv.visitTypeInsn(Opcodes.CHECKCAST, original.getInternalName());
					}
					return;
				} else if ((opcode == Opcodes.PUTFIELD && this.method.is(MethodType.PRIMARY_CONSTRUCTOR)) || (opcode == Opcodes.PUTSTATIC && this.method.is(MethodType.STATIC_INITIALIZER))) {
					if (isPrimitive(original)) {
						Type wrapper = convertToWrapper(original);
						this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper.getInternalName(), "valueOf", "(" + original.getDescriptor() + ")" + wrapper.getDescriptor(), false);
						original = wrapper;
					}
					Label start = new Label();
					Label end = new Label();
					
					int local = newLocal(this.mv, original);
					this.mv.visitVarInsn(Opcodes.ASTORE, local);
					this.insertLabel(start);
					this.mv.visitVarInsn(Opcodes.ALOAD, local);
					
					Type dynamic = Type.getType("(" + original.getDescriptor() + ")" + SUPPLIER.getDescriptor());
					this.mv.visitInvokeDynamicInsn("get", dynamic.getDescriptor(), METAFACTORY_HANDLE, Type.getType("()Ljava/lang/Object;"), this.createFactoryHandle(original), Type.getType("()" + original.getDescriptor()));
					super.visitFieldInsn(opcode, owner, name, SUPPLIER.getDescriptor());
					this.insertLabel(end);
					this.visitLocalVariable(local, "generated$LazyTransformer$Temp" + local, original, null, start, end);
					return;
				}
			}
			super.visitFieldInsn(opcode, owner, name, descriptor);
		}
		
		private @NotNull Handle createFactoryHandle(@NotNull Type original) {
			Type factory = Type.getType("(" + original.getDescriptor() + ")" + original.getDescriptor());
			String name = "generated$Factory$" + original.getClassName().replace(".", "$");
			this.factory.accept(name, factory);
			return new Handle(Opcodes.H_INVOKESTATIC, this.method.getOwner().getInternalName(), name, factory.getDescriptor(), false);
		}
	}
	
	private static class StaticInitializerMethodVisitor extends LazyMethodVisitor {
		
		private final Map</*Field Name*/String, /*Initial Value*/Object> converts;
		
		private StaticInitializerMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method, @NotNull Map<String, Set<String>> lookup, @NotNull Map<String, Object> converts, @NotNull BiConsumer<String, Type> factory) {
			super(visitor, method, lookup, factory);
			this.converts = converts;
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			for (Map.Entry<String, Object> entry : this.converts.entrySet()) {
				Object value = entry.getValue();
				if (value instanceof Number number) {
					loadNumber(this, number);
				} else {
					this.visitLdcInsn(value);
				}
				this.visitFieldInsn(Opcodes.PUTSTATIC, this.method.getOwner().getInternalName(), entry.getKey(), convertToPrimitive(Type.getType(value.getClass())).getDescriptor());
			}
		}
		
		@Override
		public void visitEnd() {
			super.visitEnd();
			this.converts.clear();
		}
	}
}
