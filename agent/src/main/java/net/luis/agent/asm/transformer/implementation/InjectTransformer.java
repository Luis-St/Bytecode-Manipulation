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
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.util.*;

import static net.luis.agent.asm.Instrumentations.*;
import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InjectTransformer extends BaseClassTransformer {
	
	private static final String IMPLEMENTATION_ERROR = "Inject Implementation Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = InterfaceTransformer.createLookup(INJECT_INTERFACE);
	
	public InjectTransformer() {
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
		return new InjectClassVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class InjectClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Method Signature*/String, List<InjectData>> injects = new HashMap<>();
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private InjectClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map<String, List<String>> lookup) {
			super(writer, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				Class targetClass = Agent.getClass(Type.getObjectType(name));
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					Class ifaceClass = Agent.getClass(iface);
					for (Method method : ifaceClass.getMethods().values()) {
						if (method.isAnnotatedWith(INJECT)) {
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
			if (!ifaceMethod.is(TypeAccess.PUBLIC)) {
				throw CrashReport.create("Method annotated with @Inject must be public", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Inject must be default implemented", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature).exception();
			}
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Inject must not throw exceptions", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature)
					.addDetail("Exceptions", ifaceMethod.getExceptions()).exception();
			}
			Method existingMethod = targetClass.getMethod(ifaceMethod.getSignature(SignatureType.FULL));
			if (existingMethod != null) {
				throw CrashReport.create("Target class of inject already has method with same signature", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature)
					.addDetail("Existing Method", existingMethod.getSignature(SignatureType.DEBUG)).exception();
			}
			String injectName = this.getInjectName(ifaceMethod);
			List<Method> possibleMethod = ASMUtils.getBySignature(injectName, targetClass);
			if (possibleMethod.isEmpty()) {
				throw CrashReport.create("Could not find method specified in inject", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature).addDetail("Method", injectName)
					.addDetail("Possible Methods", targetClass.getMethods(this.getRawInjectName(injectName)).stream().map(Method::toString).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for inject", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature).addDetail("Method", injectName)
					.addDetail("Possible Methods", possibleMethod.stream().map(Method::toString).toList()).exception();
			}
			Method method = possibleMethod.getFirst();
			if (!ifaceMethod.is(TypeModifier.STATIC) && method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Inject is declared none-static, but specified a static method", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature)
					.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				for (Parameter parameter : ifaceMethod.getParameters().values()) {
					if (!parameter.isAnnotatedWith(THIS) && !parameter.isAnnotatedWith(LOCAL)) {
						throw CrashReport.create("Parameter of inject must be annotated with @This or @Local", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature)
							.addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName()).exception();
					}
					if (method.is(TypeModifier.STATIC) && parameter.isAnnotatedWith(THIS)) {
						throw CrashReport.create("Parameter of inject cannot be annotated with @This, because the specified method is static", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner())
							.addDetail("Inject", signature).addDetail("Parameter Index", parameter.getIndex()).addDetail("Parameter Type", parameter.getType()).addDetail("Parameter Name", parameter.getName())
							.addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
					}
				}
			}
			if (method.returns(VOID)) {
				if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(BOOLEAN)) {
					throw CrashReport.create("Method annotated with @Inject specified a method which returns void, but inject method does not return void or boolean (cancellation)", IMPLEMENTATION_ERROR)
						.addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature).addDetail("Inject Return Type", method.getReturnType()).addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
				}
			} else if (method.returnsAny(PRIMITIVES)) {
				Type methodWrapper = convertToWrapper(method.getReturnType());
				if (!ifaceMethod.returns(methodWrapper)) {
					throw CrashReport.create("Method annotated with @Inject specified a method which returns a primitive type, but inject method does not return the corresponding wrapper type", IMPLEMENTATION_ERROR)
						.addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature).addDetail("Inject Return Type", methodWrapper).addDetail("Method", method.getSignature(SignatureType.DEBUG))
						.addDetail("Method Return Type", method.getReturnType()).exception();
				}
			} else if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(method.getReturnType())) {
				throw CrashReport.create("Method annotated with @Inject must either return void or the same type as the specified method", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature)
					.addDetail("Inject Return Type", ifaceMethod.getReturnType()).addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Method Return Type", method.getReturnType()).exception();
			}
			
			Annotation annotation = Objects.requireNonNull(ifaceMethod.getAnnotation(INJECT).get("target"));
			TargetClassScanner scanner = new TargetClassScanner(method, annotation);
			ClassFileScanner.scanClass(this.type, scanner);
			if (!scanner.visitedTarget()) {
				throw CrashReport.create("Could not find method specified in inject during scan of its own class", IMPLEMENTATION_ERROR).addDetail("Scanner", scanner.getClass().getName()).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Inject", signature).addDetail("Scanned Class", targetClass.getType()).addDetail("Method", method.getSignature(SignatureType.DEBUG)).exception();
			}
			int line = scanner.getTargetLine();
			Method lambdaMethod = scanner.getLambdaMethod();
			if (lambdaMethod != null && !ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Inject is declared none-static, but specified a lambda expression", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner())
					.addDetail("Inject", signature).addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Lambda", lambdaMethod.getSignature(SignatureType.DEBUG)).exception();
			}
			if (line == -1) {
				throw CrashReport.create("Could not find target in method body of method specified in inject", IMPLEMENTATION_ERROR).addDetail("Interface", ifaceMethod.getOwner()).addDetail("Inject", signature)
					.addDetail("Method", method.getSignature(SignatureType.DEBUG)).addDetail("Lambda", lambdaMethod).addDetail("Target", annotation.get("value")).addDetail("Target Type", annotation.get("type"))
					.addDetail("Target Mode", annotation.getOrDefault("mode")).addDetail("Target Ordinal", annotation.getOrDefault("ordinal")).addDetail("Target Offset", annotation.getOrDefault("offset")).removeNullValues(true).exception();
			}
			
			String key = (lambdaMethod != null ? lambdaMethod : method).getSignature(SignatureType.FULL);
			this.injects.computeIfAbsent(key, m -> new ArrayList<>()).add(new InjectData(line, ifaceMethod, method, lambdaMethod));
		}
		
		@Override
		public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			if (this.injects.containsKey(name + descriptor)) {
				this.markModified();
				return new InjectMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), this.injects.get(name + descriptor));
			}
			return visitor;
		}
		
		//region Helper methods
		private @NotNull String getInjectName(@NotNull Method ifaceMethod) {
			Annotation annotation = ifaceMethod.getAnnotation(INJECT);
			String target = annotation.get("method");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.getName();
			if (methodName.startsWith("inject")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private @NotNull String getRawInjectName(@NotNull String target) {
			if (target.contains("(")) {
				return target.substring(0, target.indexOf('('));
			}
			return target;
		}
		//endregion
	}
	
	private static class InjectMethodVisitor extends LabelTrackingMethodVisitor {
		
		private final Map</*Line Number*/Integer, List<InjectData>> injects = new HashMap<>();
		private int lastLine = -1;
		
		private InjectMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull List<InjectData> injects) {
			super(visitor);
			for (InjectData data : injects) {
				this.injects.computeIfAbsent(data.line(), l -> new ArrayList<>()).add(data);
			}
		}
		
		private void instrumentInLine(int line) {
			List<InjectData> injects = this.injects.remove(line);
			if (injects != null) {
				injects.forEach(this::instrumentInject);
			}
		}
		
		@Override
		public void visitLabel(@NotNull Label label) {
			int line = ASMUtils.getLine(label);
			if (line != -1) {
				if (this.lastLine != -1 && line - this.lastLine > 1) {
					for (int i = this.lastLine + 1; i < line; i++) {
						this.instrumentInLine(i);
					}
				}
			}
			super.visitLabel(label);
		}
		
		@Override
		public void visitLineNumber(int line, @NotNull Label start) {
			super.visitLineNumber(line, start);
			if (this.lastLine != -1 && line - this.lastLine > 1) {
				for (int i = this.lastLine + 1; i < line; i++) {
					if (this.injects.containsKey(i)) {
						//region Crash report
						List<InjectData> injects = this.injects.get(i);
						if (injects.size() == 1) {
							InjectData inject = injects.getFirst();
							throw CrashReport.create("Inject was not instrumented correctly, because no label was found", IMPLEMENTATION_ERROR).addDetail("Line", i).addDetail("Interface", inject.ifaceMethod().getOwner())
								.addDetail("Inject", inject.ifaceMethod().getSignature(SignatureType.DEBUG)).addDetail("Method", inject.method().getSignature(SignatureType.DEBUG)).addDetail("Lambda Method", inject.lambdaMethod()).removeNullValues(true).exception();
						} else {
							CrashReport report = CrashReport.create("Injects were not instrumented correctly, because no label was found", IMPLEMENTATION_ERROR);
							
							List<Map<String, Object>> details = new ArrayList<>();
							for (InjectData inject : injects) {
								Map<String, Object> detail = new HashMap<>();
								detail.put("Interface", inject.ifaceMethod().getOwner());
								detail.put("Inject", inject.ifaceMethod().getSignature(SignatureType.DEBUG));
								detail.put("Method", inject.method().getSignature(SignatureType.DEBUG));
								if (inject.lambdaMethod() != null) {
									detail.put("Lambda Method", inject.lambdaMethod().getSignature(SignatureType.DEBUG));
								}
								details.add(detail);
							}
							throw report.addDetail("Line", i).addDetail("Injects", details).exception();
						}
						//endregion
					}
				}
			}
			this.instrumentInLine(line);
			this.lastLine = line;
		}
		
		//region Instrumentation
		private void instrumentInject(@NotNull InjectData inject) {
			if (inject.lambdaMethod() != null) {
				if (!inject.lambdaMethod().returns(VOID)) {
					throw CrashReport.create("Inject into a lambda expression must return void", IMPLEMENTATION_ERROR).addDetail("Inject", inject.ifaceMethod().getSignature(SignatureType.DEBUG))
						.addDetail("Method", inject.method().getSignature(SignatureType.DEBUG)).addDetail("Lambda Method", inject.lambdaMethod().getSignature(SignatureType.DEBUG)).exception();
				}
				this.instrumentInjectAsListener(inject.ifaceMethod(), inject.lambdaMethod());
				inject.ifaceMethod().getAnnotation(INJECT).getValues().put("lambda", true);
			} else if (inject.ifaceMethod().returns(VOID)) {
				this.instrumentInjectAsListener(inject.ifaceMethod(), inject.method());
			} else if (inject.method().returns(convertToPrimitive(inject.ifaceMethod().getReturnType()))) {
				this.instrumentInjectAsCallback(inject.ifaceMethod(), inject.method());
			} else if (inject.ifaceMethod().returns(BOOLEAN)) {
				this.instrumentInjectAsCancellation(inject.ifaceMethod(), inject.method());
			} else {
				throw CrashReport.create("Unknown how to implement inject, tried as listener, callback and cancellation but failed", IMPLEMENTATION_ERROR).addDetail("Inject", inject.ifaceMethod().getSignature(SignatureType.DEBUG))
					.addDetail("Method", inject.method().getSignature(SignatureType.DEBUG)).addDetail("Line", inject.line()).exception();
			}
		}
		
		private void instrumentInjectAsListener(@NotNull Method ifaceMethod, @NotNull Method method) {
			this.instrumentMethodCall(ifaceMethod, method);
		}
		
		private void instrumentInjectAsCallback(@NotNull Method ifaceMethod, @NotNull Method method) {
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodCall(ifaceMethod, method);
			int local = newLocal(this.mv, method.getReturnType());
			this.mv.visitVarInsn(Opcodes.ASTORE, local);
			this.insertLabel(start);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNULL, end);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			if (method.returnsAny(PRIMITIVES)) {
				Type primitive = method.getReturnType();
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ifaceMethod.getReturnType().getInternalName(), primitive.getClassName() + "Value", "()" + primitive.getDescriptor(), false);
			}
			this.mv.visitInsn(method.getReturnType().getOpcode(Opcodes.IRETURN));
			this.mv.visitJumpInsn(Opcodes.GOTO, end);
			this.insertLabel(end);
			this.visitLocalVariable(local, "generated$InjectTransformer$Temp" + local, method.getReturnType(), null, start, end);
		}
		
		private void instrumentInjectAsCancellation(@NotNull Method ifaceMethod, @NotNull Method method) {
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodCall(ifaceMethod, method);
			int local = newLocal(this.mv, BOOLEAN);
			this.mv.visitVarInsn(Opcodes.ISTORE, local);
			this.insertLabel(start);
			this.mv.visitVarInsn(Opcodes.ILOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNE, end);
			this.mv.visitInsn(Opcodes.RETURN);
			this.mv.visitJumpInsn(Opcodes.GOTO, start);
			this.insertLabel(end);
			this.visitLocalVariable(local, "generated$InjectTransformer$Temp" + local, BOOLEAN, null, start, end);
		}
		
		private void instrumentMethodCall(@NotNull Method ifaceMethod, @NotNull Method method) {
			boolean isInstance = !ifaceMethod.is(TypeModifier.STATIC);
			if (isInstance) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			for (Parameter parameter : ifaceMethod.getParameters().values()) {
				this.mv.visitVarInsn(parameter.getType().getOpcode(Opcodes.ILOAD), getLoadIndex("inject", parameter, ifaceMethod, method, this.getScopeIndex()));
			}
			this.mv.visitMethodInsn(isInstance ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKESTATIC, ifaceMethod.getOwner().getInternalName(), ifaceMethod.getName(), ifaceMethod.getType().getDescriptor(), true);
		}
		//endregion
	}
	
	private record InjectData(int line, @NotNull Method ifaceMethod, @NotNull Method method, @Nullable Method lambdaMethod) {}
}
