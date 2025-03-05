package net.luis.agent.asm.scanner;

import net.luis.agent.Agent;
import net.luis.agent.asm.data.Method;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.luis.agent.asm.Instrumentations.*;

/**
 *
 * @author Luis-St
 *
 */

public class LambdaMethodScanner extends ClassVisitor {
	
	private final List<Method> lambdaMethods = new ArrayList<>();
	private final Method method;
	
	public LambdaMethodScanner(@NotNull Method method) {
		super(Opcodes.ASM9);
		this.method = method;
	}
	
	@Override
	public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String genericSignature, String @Nullable [] exceptions) {
		if (this.method.is(this.method.getOwner(), name, Type.getType(descriptor))) {
			return new LambdaMethodVisitor(this.method, this.lambdaMethods::add);
		}
		return super.visitMethod(access, name, descriptor, genericSignature, exceptions);
	}
	
	public @NotNull List<Method> getLambdaMethods() {
		return this.lambdaMethods;
	}
	
	private static final class LambdaMethodVisitor extends MethodVisitor {
		
		private final Method method;
		private final Consumer<Method> callback;
		
		private LambdaMethodVisitor(@NotNull Method method, @NotNull Consumer<Method> callback) {
			super(Opcodes.ASM9);
			this.method = method;
			this.callback = callback;
		}
		
		@Override
		public void visitInvokeDynamicInsn(@NotNull String name, @NotNull String descriptor, @NotNull Handle handle, Object @NotNull ... arguments) {
			if (METAFACTORY_HANDLE.equals(handle)) {
				for (Object argument : arguments) {
					if (argument instanceof Handle targetHandle) {
						Type owner = Type.getObjectType(targetHandle.getOwner());
						if (this.method.getOwner().equals(owner)) {
							Method method = Agent.getClass(owner).getMethod(targetHandle.getName() + targetHandle.getDesc());
							if (method != null) {
								this.callback.accept(method);
							}
						}
					}
				}
			}
		}
	}
}
