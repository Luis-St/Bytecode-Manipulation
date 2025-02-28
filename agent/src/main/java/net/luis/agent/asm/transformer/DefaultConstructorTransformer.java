package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.SignatureType;
import net.luis.agent.asm.type.TypeAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.List;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

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
			CrashReport report = CrashReport.create(REPORT_CATEGORY);
			
			if (constructors.size() > 1) {
				throw report.addDetail("Constructors", constructors.stream().map(m -> m.getSignature(SignatureType.DEBUG)).toList()).exception("Class annotated with @DefaultConstructor must only have one constructor");
			}
			
			Method constructor = constructors.getFirst();
			if (constructor.getParameterCount() > 0) {
				throw report.addDetail("Constructor", constructor.getSignature(SignatureType.DEBUG)).exception("Constructor annotated with @DefaultConstructor must not have parameters");
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
