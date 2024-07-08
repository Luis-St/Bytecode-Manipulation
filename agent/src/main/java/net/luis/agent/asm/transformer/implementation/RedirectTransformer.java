package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.Agent;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.*;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.scanner.ClassFileScanner;
import net.luis.agent.asm.scanner.TargetClassScanner;
import net.luis.agent.asm.type.*;
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

public class RedirectTransformer extends BaseClassTransformer {
	
	private static final String REPORT_CATEGORY = "Redirect Implementation Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	public RedirectTransformer() {
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
		return new RedirectClassVisitor(writer, type, this.lookup, () -> this.modified = true);
	}
	
	private static class RedirectClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Method Signature*/String, List<RedirectData>> redirects = new HashMap<>();
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private RedirectClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Map<String, List<String>> lookup, @NotNull Runnable markModified) {
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
						if (method.isAnnotatedWith(REDIRECT)) {
							this.validateMethod(method, targetClass);
						} else if (method.is(TypeAccess.PUBLIC)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getSignature(SignatureType.DEBUG)).exception();
							} else if (method.getAnnotations().values().stream().map(Annotation::getType).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getSignature(SignatureType.DEBUG)).exception();
							}
						}
					}
				}
			}
		}
		
		private void validateMethod(@NotNull Method ifaceMethod, @NotNull Class targetClass) {
			String signature = ifaceMethod.getSignature(SignatureType.DEBUG);
			//region Base validation
			if (!ifaceMethod.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Redirect must be public", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Redirect must be default implemented", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature).exception();
			}
			//endregion
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Redirect must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature)
					.addDetail("Exceptions", ifaceMethod.getExceptions()).exception();
			}
			Method existingMethod = targetClass.getMethod(ifaceMethod.getSignature(SignatureType.FULL));
			if (existingMethod != null) {
				throw CrashReport.create("Target class of redirect already has method with same signature", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature)
					.addDetail("Existing Method", existingMethod.getSignature(SignatureType.DEBUG)).exception();
			}
			String redirectName = this.getRedirectName(ifaceMethod);
			List<Method> possibleMethod = ASMUtils.getBySignature(redirectName, targetClass);
			if (possibleMethod.isEmpty()) {
				throw CrashReport.create("Could not find method specified in redirect", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature).addDetail("Method", redirectName)
					.addDetail("Possible Methods", targetClass.getMethods(this.getRawRedirectName(redirectName)).stream().map(Method::toString).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for redirect", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature).addDetail("Method", redirectName)
					.addDetail("Possible Methods", possibleMethod.stream().map(Method::toString).toList()).exception();
			}
			Method method = possibleMethod.getFirst();
			if (!ifaceMethod.is(TypeModifier.STATIC) && method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Redirect is declared none-static, but specified a static method", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature)
					.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				for (Parameter parameter : ifaceMethod.getParameters().values()) {
					if (!parameter.isAnnotatedWith(THIS) && !parameter.isAnnotatedWith(LOCAL)) {
						throw CrashReport.create("Parameter of redirect must be annotated with @This or @Local", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
					}
					if (method.is(TypeModifier.STATIC) && parameter.isAnnotatedWith(THIS)) {
						throw CrashReport.create("Parameter of redirect cannot be annotated with @This, because the specified method is static", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
							.addDetail("Redirect", signature).addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
					}
				}
			}
			Annotation annotation = Objects.requireNonNull(ifaceMethod.getAnnotation(REDIRECT).get("target"));
			TargetType target = TargetType.valueOf(annotation.get("type"));
			if (target != TargetType.NEW && target != TargetType.INVOKE) {
				throw CrashReport.create("Unsupported target type specified in redirect, supported are New and Invoke", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature)
					.addDetail("Target Type", target).exception();
			}
			TargetClassScanner scanner = new TargetClassScanner(method, annotation);
			ClassFileScanner.scanClass(this.type, scanner);
			if (!scanner.visitedTarget()) {
				throw CrashReport.create("Could not find method specified in redirect during scan of its own class", REPORT_CATEGORY).addDetail("Scanner", scanner.getClass().getName()).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Redirect", signature).addDetail("Scanned Class", targetClass.getType()).addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
			}
			int line = scanner.getTargetLine();
			Method lambdaMethod = scanner.getLambdaMethod();
			if (lambdaMethod != null && !ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Redirect is declared none-static, but specified a lambda expression", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Redirect", signature).addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Lambda", lambdaMethod.getSignature(SignatureType.DEBUG)).exception();
			}
			if (line == -1) {
				throw CrashReport.create("Could not find target in method body of method specified in redirect", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Redirect", signature)
					.addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Lambda", lambdaMethod).addDetail("Target", annotation.get("value")).addDetail("Target Type", annotation.get("type"))
					.addDetail("Target Mode", annotation.getOrDefault("mode")).addDetail("Target Ordinal", annotation.getOrDefault("ordinal")).removeNullValues(true).exception();
			}
			String key = (lambdaMethod != null ? lambdaMethod : method).getSignature(SignatureType.FULL);
			this.redirects.computeIfAbsent(key, m -> new ArrayList<>()).add(new RedirectData(line, ifaceMethod, lambdaMethod != null, annotation.getOrDefault("value"), target, annotation.getOrDefault("ordinal")));
		}
		
		@Override
		public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			String fullSignature = name + descriptor;
			Method method = Agent.getClass(this.type).getMethod(fullSignature);
			if (this.redirects.containsKey(fullSignature) && method != null) {
				this.markModified();
				return new RedirectMethodVisitor(visitor, method, this.redirects.get(fullSignature));
			}
			return visitor;
		}
		
		//region Helper methods
		private @NotNull String getRedirectName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(REDIRECT);
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
		
		private @NotNull String getRawRedirectName(@NotNull String target) {
			if (target.contains("(")) {
				return target.substring(0, target.indexOf('('));
			}
			return target;
		}
		//endregion
	}
	
	private static class RedirectMethodVisitor extends LabelTrackingMethodVisitor {
		
		private final Map</*Line Number*/Integer, List<RedirectData>> redirects = new HashMap<>();
		private final Method method;
		private int line = -1;
		
		private RedirectMethodVisitor(@NotNull MethodVisitor visitor, @NotNull Method method, @NotNull List<RedirectData> redirects) {
			super(visitor);
			this.method = method;
			for (RedirectData data : redirects) {
				this.redirects.computeIfAbsent(data.line(), l -> new ArrayList<>()).add(data);
			}
		}
		
		@Override
		public void visitLineNumber(int line, @NotNull Label start) {
			super.visitLineNumber(line, start);
			if (this.redirects.containsKey(line)) {
				this.line = line;
			} else {
				this.line = -1;
			}
		}
		
		@Override
		public void visitMethodInsn(int opcode, @NotNull String o, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
			if (this.line == -1) {
				this.mv.visitMethodInsn(opcode, o, name, descriptor, isInterface);
				return;
			}
			Type owner = Type.getObjectType(o);
			Type type = Type.getType(descriptor);
			
			boolean instrumented = false;
			for (int i = 0; i < this.redirects.get(this.line).size(); i++) {
				RedirectData redirect = this.redirects.get(this.line).get(i);
				if (redirect.target() == TargetType.NEW && !"<init>".equals(name)) {
					continue;
				} else if (redirect.target() == TargetType.INVOKE && "<init>".equals(name)) {
					continue;
				}
				
				if (redirect.target() == TargetType.NEW && this.checkNew(redirect.ifaceMethod(), redirect.value(), owner, name, type)) {
					instrumented = true;
				} else if (redirect.target() == TargetType.INVOKE && this.checkInvoke(redirect.ifaceMethod(), redirect.value(), owner, name, type)) {
					instrumented = true;
				}
				
				if (instrumented) {
					Arrays.stream(Utils.reverse(type.getArgumentTypes())).forEach(t -> pop(this.mv, t));
					if (opcode != Opcodes.INVOKESTATIC) {
						this.mv.visitInsn(Opcodes.POP);
					}
					this.instrumentRedirect(redirect.ifaceMethod());
					if (redirect.lambda()) {
						redirect.ifaceMethod().getAnnotation(REDIRECT).getValues().put("lambda", true);
					}
					break;
				}
			}
			if (!instrumented) {
				this.mv.visitMethodInsn(opcode, o, name, descriptor, isInterface);
			}
		}
		
		//region Instrumentation
		private void instrumentRedirect(@NotNull Method ifaceMethod) {
			if (!ifaceMethod.is(TypeModifier.STATIC)) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			for (Parameter parameter : ifaceMethod.getParameters().values()) {
				this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), getLoadIndex("redirect", parameter, ifaceMethod, this.method, this.getScopeIndex()));
			}
			this.mv.visitMethodInsn(ifaceMethod.is(TypeModifier.STATIC) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEINTERFACE, ifaceMethod.getOwner().getInternalName(), ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), true);
		}
		//endregion
		
		//region Helper methods
		private boolean checkNew(@NotNull Method ifaceMethod, @NotNull String value, @NotNull Type owner, @NotNull String name, @NotNull Type type) {
			if (!ASMUtils.matchesTarget(value, owner, name, type) && !isSameType(owner, value)) {
				return false;
			}
			if (!ifaceMethod.returns(owner)) {
				throw CrashReport.create("Redirect must return the type which is created by the specified constructor", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Redirect", ifaceMethod.getSignature(SignatureType.DEBUG)).addDetail("Redirect Return Type", ifaceMethod.getReturnType()).addDetail("Created Type", owner).exception();
			}
			return true;
		}
		
		private boolean checkInvoke(@NotNull Method ifaceMethod, @NotNull String value, @NotNull Type owner, @NotNull String name, @NotNull Type type) {
			if (!ASMUtils.matchesTarget(value, owner, name, type)) {
				return false;
			}
			if (!ifaceMethod.returns(type.getReturnType())) {
				throw CrashReport.create("Redirect must return the same type as the specified method", REPORT_CATEGORY).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Redirect", ifaceMethod.getSignature(SignatureType.DEBUG)).addDetail("Redirect Return Type", ifaceMethod.getReturnType()).addDetail("Method Return Type", type.getReturnType()).exception();
			}
			return true;
		}
		//endregion
	}
	
	private record RedirectData(int line, @NotNull Method ifaceMethod, boolean lambda, @NotNull String value, @NotNull TargetType target, int ordinal) {}
}
