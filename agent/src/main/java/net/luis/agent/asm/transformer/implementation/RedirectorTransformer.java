package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.Types;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.ContextBasedClassVisitor;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.TypeAccess;
import net.luis.agent.asm.type.TypeModifier;
import net.luis.agent.util.TargetType;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 * @author Luis-St
 */

public class RedirectorTransformer extends BaseClassTransformer {
	
	private static final String REPORT_CATEGORY = "Redirector Implementation Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(INJECT_INTERFACE);
	
	public RedirectorTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		return new RedirectorClassVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class RedirectorClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final Map</*Method Signature*/String, List<RedirectorData>> redirectors = new HashMap<>();
		
		private RedirectorClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				Class targetClass = Agent.getClass(Type.getObjectType(name));
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					Class ifaceClass = Agent.getClass(iface);
					for (Method method : ifaceClass.getMethods().values()) {
						if (method.isAnnotatedWith(REDIRECTOR)) {
							this.validateMethod(method, targetClass);
						} else if (method.is(TypeAccess.PUBLIC)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getSourceSignature(true)).exception();
							} else if (method.getAnnotations().values().stream().map(Annotation::getType).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getSourceSignature(true)).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateMethod(@NotNull Method ifaceMethod, @NotNull Class targetClass) {
			String signature = ifaceMethod.getSourceSignature(true);
			//region Base validation
			if (!ifaceMethod.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Redirector must be public", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Redirector must be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature).exception();
			}
			//endregion
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Redirector must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature)
					.addDetail("Exceptions", ifaceMethod.getExceptions()).exception();
			}
			Method existingMethod = targetClass.getMethod(ifaceMethod.getFullSignature());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of redirector already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature)
					.addDetail("Existing Method", existingMethod.getSourceSignature(true)).exception();
			}
			String redirectorName = this.getRedirectorName(ifaceMethod);
			List<Method> possibleMethod = ASMUtils.getBySignature(redirectorName, targetClass);
			if (possibleMethod.isEmpty()) {
				throw CrashReport.create("Could not find method specified in redirector", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature).addDetail("Method", redirectorName)
					.addDetail("Possible Methods", targetClass.getMethods(this.getRawRedirectorName(redirectorName)).stream().map(Method::toString).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for redirector", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature).addDetail("Method", redirectorName)
					.addDetail("Possible Methods", possibleMethod.stream().map(Method::toString).toList()).exception();
			}
			Method method = possibleMethod.getFirst();
			if (!ifaceMethod.is(TypeModifier.STATIC) && method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Redirector is declared none-static, but specified a static method", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature)
					.addDetail("Method", method.getSourceSignature(true)).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				for (Parameter parameter : ifaceMethod.getParameters().values()) {
					if (!parameter.isAnnotatedWith(THIS) && !parameter.isAnnotatedWith(LOCAL)) {
						throw CrashReport.create("Parameter of redirector must be annotated with @This or @Local", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
					}
					if (method.is(TypeModifier.STATIC) && parameter.isAnnotatedWith(THIS)) {
						throw CrashReport.create("Parameter of redirector cannot be annotated with @This, because the specified method is static", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
							.addDetail("Redirector", signature).addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Method", method.getSourceSignature(true)).exception();
					}
				}
			}
			
			Annotation annotation = Objects.requireNonNull(ifaceMethod.getAnnotation(REDIRECTOR).get("target"));
			TargetType target = TargetType.valueOf(annotation.get("type"));
			if (target != TargetType.NEW && target != TargetType.INVOKE) {
				throw CrashReport.create("Unsupported target type specified in redirector", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirector", signature).addDetail("Target Type", target).exception();
			}
			
			this.redirectors.computeIfAbsent(method.getFullSignature(), m -> new ArrayList<>()).add(new RedirectorData(ifaceMethod, annotation.getOrDefault("value"), target, annotation.getOrDefault("ordinal")));
		}
		
		@Override
		public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			
			String fullSignature = name + descriptor;
			Method method = Agent.getClass(this.type).getMethod(fullSignature);
			if (this.redirectors.containsKey(fullSignature) && method != null) {
				this.markModified();
				return new RedirectorMethodVisitor(visitor, method, this.redirectors.get(fullSignature));
			}
			return visitor;
		}
		
		//region Helper methods
		private @NotNull String getRedirectorName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(REDIRECTOR);
			String target = annotation.get("method");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.getName();
			if (methodName.startsWith("redirect")) {
				return Utils.uncapitalize(methodName.substring(8));
			}
			return methodName;
		}
		
		private @NotNull String getRawRedirectorName(@NotNull String target) {
			if (target.contains("(")) {
				return target.substring(0, target.indexOf('('));
			}
			return target;
		}
		//endregion
	}
	
	private static class RedirectorMethodVisitor extends MethodVisitor {
		
		private final Method method;
		private final List<RedirectorData> redirectors;
		
		private RedirectorMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method, @NotNull List<RedirectorData> redirectors) {
			super(Opcodes.ASM9, visitor);
			this.method = method;
			this.redirectors = redirectors;
		}
		
		@Override
		public void visitMethodInsn(int opcode, @NotNull String o, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
			Type owner = Type.getObjectType(o);
			Type type = Type.getType(descriptor);
			
			int redirect = -1;
			for (int i = 0; i < this.redirectors.size(); i++) {
				RedirectorData redirector = this.redirectors.get(i);
				if (redirector.target() == TargetType.NEW && !"<init>".equals(name)) {
					continue;
				} else if (redirector.target() == TargetType.INVOKE && "<init>".equals(name)) {
					continue;
				}
				
				Method ifaceMethod = redirector.ifaceMethod();
				if (redirector.target() == TargetType.NEW && this.checkNew(ifaceMethod, redirector.value(), owner, name, type)) {
					redirect = i;
				} else if (redirector.target() == TargetType.INVOKE && this.checkInvoke(ifaceMethod, redirector.value(), owner, name, type)) {
					redirect = i;
				}
				
				if (redirect != -1) {
					Arrays.stream(Utils.reverse(type.getArgumentTypes())).forEach(t -> pop(this.mv, t));
					if (opcode != Opcodes.INVOKESTATIC) {
						this.mv.visitInsn(Opcodes.POP);
					}
					this.instrumentRedirect(ifaceMethod, owner, type);
					break;
				}
			}
			
			if (redirect == -1) {
				this.mv.visitMethodInsn(opcode, o, name, descriptor, isInterface);
			} else {
				this.redirectors.remove(redirect);
			}
		}
		
		//region Instrumentation
		private void instrumentRedirect(@NotNull Method ifaceMethod, @NotNull Type owner, @NotNull Type current) {
			if (!ifaceMethod.is(TypeModifier.STATIC)) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			for (Parameter parameter : ifaceMethod.getParameters().values()) {
				this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), getLoadIndex("redirector", parameter, ifaceMethod, this.method));
			}
			this.mv.visitMethodInsn(ifaceMethod.is(TypeModifier.STATIC) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE, ifaceMethod.getOwner().getInternalName(), ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), true);
		}
		//endregion
		
		//region Helper methods
		private boolean checkNew(@NotNull Method ifaceMethod, @NotNull String value, @NotNull Type owner, @NotNull String name, @NotNull Type type) {
			if (!ASMUtils.matchesTarget(value, owner, name, type) && !Types.isSameType(owner, value)) {
				return false;
			}
			if (!ifaceMethod.returns(owner)) {
				throw CrashReport.create("Redirector must return the type which is created by the specified constructor", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Redirector", ifaceMethod.getSourceSignature(true)).addDetail("Redirector Return Type", ifaceMethod.getReturnType()).addDetail("Created Type", owner).exception();
			}
			return true;
		}
		
		private boolean checkInvoke(@NotNull Method ifaceMethod, @NotNull String value, @NotNull Type owner, @NotNull String name, @NotNull Type type) {
			if (!ASMUtils.matchesTarget(value, owner, name, type)) {
				return false;
			}
			if (!ifaceMethod.returns(type.getReturnType())) {
				throw CrashReport.create("Redirector must return the same type as the specified method", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Redirector", ifaceMethod.getSourceSignature(true)).addDetail("Redirector Return Type", ifaceMethod.getReturnType()).addDetail("Method Return Type", type.getReturnType()).exception();
			}
			return true;
		}
		//endregion
	}
	
	private static record RedirectorData(@NotNull Method ifaceMethod, @NotNull String value, @NotNull TargetType target, int ordinal) {}
}
