package net.luis.agent.asm.transformer.method;

import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.type.MethodType;
import net.luis.agent.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.HashMap;
import java.util.Map;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class AsyncTransformer extends BaseClassTransformer {
	
	public AsyncTransformer(@NotNull PreloadContext context) {
		super(context, true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		ClassContent content = this.context.getClassContent(type);
		return content.methods().stream().anyMatch(method -> method.returns(VOID) && method.isAnnotatedWith(ASYNC));
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new AsyncClassVisitor(writer, this.context, type, this.context.getClassContent(type), () -> this.modified = true);
	}
	
	private static class AsyncClassVisitor extends BaseClassVisitor {
		
		private static final Handle METAFACTORY = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
			"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
		private static final Type VOID_METHOD = Type.getMethodType(VOID);
		
		private final ClassContent content;
		private final Map<MethodData, String> methods = new HashMap<>();
		
		private AsyncClassVisitor(@NotNull ClassVisitor visitor, @NotNull PreloadContext context, @NotNull Type type, @NotNull ClassContent content, @NotNull Runnable markModified) {
			super(visitor, context, type, markModified);
			this.content = content;
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodData method = this.content.getMethod(name, Type.getMethodType(descriptor));
			if (method == null || !method.is(MethodType.METHOD) || method.is(TypeModifier.ABSTRACT) || !method.isAnnotatedWith(ASYNC)) {
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}
			access = access & ~method.access().getOpcode();
			String newName = "generated$" + name + "$async";
			this.methods.put(method, newName);
			MethodVisitor visitor = super.visitMethod(access | Opcodes.ACC_PRIVATE, newName, descriptor, signature, exceptions);
			return new BaseMethodVisitor(visitor, this.context, this.type, method, this::markModified).skipAnnotation();
		}
		
		@Override
		public void visitEnd() {
			for (Map.Entry<MethodData, String> entry : this.methods.entrySet()) {
				MethodData method = entry.getKey();
				MethodVisitor visitor = super.visitMethod(method.getOpcodes(), method.name(), method.type().getDescriptor(), method.signature(), null);
				visitor.visitCode();
				this.instrumentMethodAnnotations(visitor, method, true);
				this.instrumentParameterAnnotations(visitor, method);
				//region Parameter loading
				int index = 0;
				if (!method.is(TypeModifier.STATIC)) {
					visitor.visitVarInsn(Opcodes.ALOAD, index++);
				}
				for (ParameterData parameter : method.parameters()) {
					visitor.visitVarInsn(parameter.type().getOpcode(Opcodes.ILOAD), index++);
				}
				//endregion
				visitor.visitInvokeDynamicInsn("run", this.makeDescriptor(method), METAFACTORY, VOID_METHOD, this.createHandle(method, entry.getValue()), VOID_METHOD);
				visitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/concurrent/CompletableFuture", "runAsync", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", false);
				visitor.visitInsn(Opcodes.POP);
				visitor.visitInsn(Opcodes.RETURN);
				visitor.visitMaxs(0, 0);
				visitor.visitEnd();
				this.markModified();
			}
		}
		
		//region Helper methods
		private @NotNull String makeDescriptor(@NotNull MethodData method) {
			StringBuilder builder = new StringBuilder();
			builder.append("(");
			if (!method.is(TypeModifier.STATIC)) {
				builder.append(this.type.getDescriptor());
			}
			for (ParameterData parameter : method.parameters()) {
				builder.append(parameter.type().getDescriptor());
			}
			builder.append(")Ljava/lang/Runnable;");
			return builder.toString();
		}
		
		private @NotNull Handle createHandle(@NotNull MethodData method, String newName) {
			return new Handle(method.is(TypeModifier.STATIC) ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKEVIRTUAL, this.type.getInternalName(), newName, method.type().getDescriptor(), false);
		}
		//endregion
	}
}
