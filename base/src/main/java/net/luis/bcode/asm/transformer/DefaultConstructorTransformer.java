package net.luis.bcode.asm.transformer;

import net.luis.bcode.Agent;
import net.luis.bcode.asm.base.BaseClassTransformer;
import net.luis.bcode.asm.base.ContextBasedClassVisitor;
import net.luis.bcode.asm.data.Class;
import net.luis.bcode.asm.data.*;
import net.luis.bcode.asm.report.CrashReport;
import net.luis.bcode.asm.type.SignatureType;
import net.luis.bcode.asm.type.TypeAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.List;

import static net.luis.bcode.asm.Instrumentations.*;
import static net.luis.bcode.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class DefaultConstructorTransformer extends BaseClassTransformer {
	
	public DefaultConstructorTransformer() {
		super(true);
	}
	
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !Agent.getClass(type).isAnnotatedWith(DEFAULT_CONSTRUCTOR);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new DefaultConstructorClassVisitor(writer, type, () -> this.modified = true);
	}
	
	private static class DefaultConstructorClassVisitor extends ContextBasedClassVisitor {
		
		private static final String REPORT_CATEGORY = "Invalid Annotated Element";
		
		private final Method constructor;
		private final TypeAccess accessModifier;
		private final boolean instancable;
		
		private DefaultConstructorClassVisitor(@NotNull ClassVisitor visitor, @NotNull Type type, @NotNull Runnable markModified) {
			super(visitor, type, markModified);
			Class clazz = Agent.getClass(type);
			List<Method> constructors = clazz.getMethods("<init>");
			if (constructors.size() > 1) {
				throw CrashReport.create("Class annotated with @DefaultConstructor must only have one constructor", REPORT_CATEGORY)
					.addDetail("Constructors", constructors.stream().map(m -> m.getSignature(SignatureType.DEBUG)).toList()).exception();
			}
			Method constructor = constructors.getFirst();
			if (constructor.getParameterCount() > 0) {
				throw CrashReport.create("Constructor annotated with @DefaultConstructor must not have parameters", REPORT_CATEGORY).addDetail("Constructor", constructor.getSignature(SignatureType.DEBUG)).exception();
			}
			this.constructor = constructor;
			
			Annotation annotation = clazz.getAnnotation(DEFAULT_CONSTRUCTOR);
			this.accessModifier = TypeAccess.valueOf(annotation.get("value"));
			this.instancable = annotation.getOrDefault("instancable");
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			if ("<init>".equals(name) && "()V".equals(descriptor)) {
				access &= ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
				access |= this.accessModifier.getOpcode();
				
				MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
				if (!this.instancable) {
					visitor = new DefaultConstructorMethodVisitor(visitor);
				}
				
				Class clazz = Agent.getClass(this.type);
				clazz.getMethods().replace(this.constructor.getSignature(SignatureType.FULL), Method.builder(this.constructor).access(this.accessModifier).build());
				this.markModified();
				return visitor;
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
	}
	
	private static class DefaultConstructorMethodVisitor extends MethodVisitor {
		
		private DefaultConstructorMethodVisitor(@NotNull MethodVisitor methodVisitor) {
			super(Opcodes.ASM9, methodVisitor);
		}
		
		@Override
		public void visitCode() {
			super.visitCode();
			instrumentThrownException(this.mv, RUNTIME_EXCEPTION, "Utility class is not instancable");
		}
	}
}
