package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.AgentContext;
import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.Instrumentations;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.scanner.ClassFileScanner;
import net.luis.agent.preload.scanner.TargetClassScanner;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.lang.reflect.Field;
import java.util.*;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InjectorTransformer extends BaseClassTransformer {
	
	private static final String IMPLEMENTATION_ERROR = "Inject Implementation Error";
	private static final String MISSING_INFORMATION = "Missing Debug Information";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(AgentContext.get(), INJECT_INTERFACE);
	
	public InjectorTransformer() {
		super(true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new InjectorClassVisitor(writer, type, () -> this.modified = true, this.lookup);
	}
	
	private static class InjectorClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final Map</*Method Signature*/String, List<InjectorData>> injectors = new HashMap<>();
		
		private InjectorClassVisitor(@NotNull ClassWriter writer, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		@SuppressWarnings("DuplicatedCode")
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				AgentContext context = AgentContext.get();
				Type target = Type.getObjectType(name);
				ClassData targetData = context.getClassData(target);
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassData ifaceData = context.getClassData(iface);
					for (MethodData method : ifaceData.methods()) {
						if (method.isAnnotatedWith(INJECTOR)) {
							this.validateMethod(iface, method, target, targetData);
						} else if (method.is(TypeAccess.PUBLIC)) {
							if (method.getAnnotations().isEmpty()) {
								throw createReport("Found method without annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							} else if (method.getAnnotations().stream().map(AnnotationData::type).noneMatch(IMPLEMENTATION_ANNOTATIONS::contains)) {
								throw createReport("Found method without valid annotation, does not know how to implement", iface, method.getMethodSignature()).exception();
							}
						}
					}
				}
			}
		}
		
		private @NotNull String getInjectorName(@NotNull MethodData ifaceMethod) {
			AnnotationData annotation = ifaceMethod.getAnnotation(INJECTOR);
			String target = annotation.get("method");
			if (target != null) {
				return target;
			}
			String methodName = ifaceMethod.name();
			if (methodName.startsWith("inject")) {
				return Utils.uncapitalize(methodName.substring(6));
			}
			return methodName;
		}
		
		private @NotNull String getRawInjectorName(@NotNull String invokerTarget) {
			if (invokerTarget.contains("(")) {
				return invokerTarget.substring(0, invokerTarget.indexOf('('));
			}
			return invokerTarget;
		}
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassData targetData) {
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("Method annotated with @Injector must be public", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Injector must be default implemented", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature).exception();
			}
			//endregion
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Injector must not throw exceptions", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Exceptions", ifaceMethod.exceptions()).exception();
			}
			MethodData existingMethod = targetData.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of injector already has method with same signature", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String injectorName = this.getInjectorName(ifaceMethod);
			List<MethodData> possibleMethod = ASMUtils.getBySignature(injectorName, targetData);
			if (possibleMethod.isEmpty()) {
				throw CrashReport.create("Could not find method specified in injector", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature).addDetail("Method", injectorName)
					.addDetail("Possible Methods", targetData.getMethods(this.getRawInjectorName(injectorName)).stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for injector", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Invoker", signature).addDetail("Method", injectorName)
					.addDetail("Possible Methods", possibleMethod.stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			MethodData method = possibleMethod.getFirst();
			if (!ifaceMethod.is(TypeModifier.STATIC) && method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Injector is declared none-static, but specified a static method", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Method", method.getMethodSignature()).exception();
			}
			if (ifaceMethod.getParameterCount() > 0) {
				for (ParameterData parameter : ifaceMethod.parameters()) {
					if (!parameter.isAnnotatedWith(THIS) && !parameter.isAnnotatedWith(LOCAL)) {
						throw CrashReport.create("Parameter of injector must be annotated with @This or @Local", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
							.addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
					}
					if (method.is(TypeModifier.STATIC) && parameter.isAnnotatedWith(THIS)) {
						throw CrashReport.create("Parameter of injector cannot be annotated with @This, because the specified method is static", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
							.addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).addDetail("Method", method.getMethodSignature()).exception();
					}
				}
			}
			if (method.returns(VOID)) {
				if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(BOOLEAN)) {
					throw CrashReport.create("Method annotated with @Injector specified a method which returns void, but injector method does not return void or boolean (cancellation)", IMPLEMENTATION_ERROR).addDetail("Interface", iface)
						.addDetail("Injector", signature).addDetail("Injector Return Type", method.getReturnType()).addDetail("Method", method.getMethodSignature()).exception();
				}
			} else if (method.returnsAny(PRIMITIVES)) {
				Type methodWrapper = convertToWrapper(method.getReturnType());
				if (!ifaceMethod.returns(methodWrapper)) {
					throw CrashReport.create("Method annotated with @Injector specified a method which returns a primitive type, but injector method does not return the corresponding wrapper type", IMPLEMENTATION_ERROR)
						.addDetail("Interface", iface).addDetail("Injector", signature).addDetail("Injector Return Type", methodWrapper).addDetail("Method", method.getMethodSignature())
						.addDetail("Method Return Type", method.getReturnType()).exception();
				}
			} else if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(method.getReturnType())) {
				throw CrashReport.create("Method annotated with @Injector must either return void or the same type as the specified method", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Injector Return Type", ifaceMethod.getReturnType()).addDetail("Method", method.getMethodSignature()).addDetail("Method Return Type", method.getReturnType()).exception();
			}
			
			AnnotationData annotation = Objects.requireNonNull(ifaceMethod.getAnnotation(INJECTOR).get("target"));
			TargetClassScanner scanner = new TargetClassScanner(method, annotation);
			ClassFileScanner.scanClass(this.type, scanner);
			if (!scanner.visitedTarget()) {
				throw CrashReport.create("Could not find method specified in injector during scan of its own class", IMPLEMENTATION_ERROR).addDetail("Scanner", scanner).addDetail("Interface", iface)
					.addDetail("Injector", signature).addDetail("Scanned Class", target).addDetail("Method", method.getMethodSignature()).exception();
			}
			int line = scanner.getLine();
			if (line == -1) {
				throw CrashReport.create("Could not find target in method body of method specified in injector", IMPLEMENTATION_ERROR).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Method", method.getMethodSignature()).addDetail("Target", annotation.get("value")).addDetail("Target Type", annotation.get("type")).addDetail("Target Mode", annotation.getOrDefault("mode"))
					.addDetail("Target Ordinal", annotation.getOrDefault("ordinal")).addDetail("Target Offset", annotation.getOrDefault("offset")).exception();
			}
			
			this.injectors.computeIfAbsent(method.getMethodSignature(), m -> new ArrayList<>()).add(new InjectorData(line, ifaceMethod, method));
		}
		
		@Override
		public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			if (this.injectors.containsKey(name + descriptor)) {
				return new InjectorMethodVisitor(new LocalVariablesSorter(access, descriptor, visitor), this.injectors.get(name + descriptor), this::markModified);
			}
			return visitor;
		}
	}
	
	private static class InjectorMethodVisitor extends MethodVisitor {
		
		private static final int FLAG_LINE_NUMBER = 128;
		private static final Field LABEL_LINE;
		private static final Field LABEL_FLAGS;
		
		private final Map</*Line Number*/Integer, List<InjectorData>> injectors;
		private final Runnable markModified;
		private int lastLine = -1;
		
		private InjectorMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull List<InjectorData> injectors, @NotNull Runnable markModified) {
			super(Opcodes.ASM9, visitor);
			this.injectors = new HashMap<>();
			this.markModified = markModified;
			for (InjectorData data : injectors) {
				this.injectors.computeIfAbsent(data.line(), l -> new ArrayList<>()).add(data);
			}
		}
		
		private void instrumentInLine(int line) {
			List<InjectorData> injectors = this.injectors.remove(line);
			if (injectors != null) {
				injectors.forEach(this::instrumentInjector);
			}
		}
		
		@Override
		public void visitLabel(@NotNull Label label) {
			int line = -1;
			try {
				short flags = LABEL_FLAGS.getShort(label);
				if ((flags & FLAG_LINE_NUMBER) != 0) {
					line = LABEL_LINE.getInt(label);
				}
			} catch (Exception ignored) {}
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
					if (this.injectors.containsKey(i)) {
						//region Crash report
						List<InjectorData> injectors = this.injectors.get(i);
						if (injectors.size() == 1) {
							InjectorData injector = injectors.getFirst();
							throw CrashReport.create("Injector was not instrumented correctly, because no label was found", IMPLEMENTATION_ERROR).addDetail("Line", i).addDetail("Interface", injector.ifaceMethod().owner())
								.addDetail("Injector", injector.ifaceMethod().getMethodSignature()).addDetail("Method", injector.method().getMethodSignature()).exception();
						} else {
							CrashReport report = CrashReport.create("Injectors were not instrumented correctly, because no label was found", IMPLEMENTATION_ERROR);
							
							List<Map<String, Object>> details = new ArrayList<>();
							for (InjectorData injector : injectors) {
								Map<String, Object> detail = new HashMap<>();
								detail.put("Interface", injector.ifaceMethod().owner());
								detail.put("Injector", injector.ifaceMethod().getMethodSignature());
								detail.put("Method", injector.method().getMethodSignature());
								details.add(detail);
							}
							throw report.addDetail("Line", i).addDetail("Injectors", details).exception();
						}
						//endregion
					}
				}
			}
			this.instrumentInLine(line);
			this.lastLine = line;
		}
		
		//region Instrumentation
		private void instrumentInjector(@NotNull InjectorData injector) {
			if (injector.ifaceMethod().returns(VOID)) {
				this.instrumentInjectorAsListener(injector.ifaceMethod(), injector.method());
			} else if (injector.method().returns(convertToPrimitive(injector.ifaceMethod().getReturnType()))) {
				this.instrumentInjectorAsCallback(injector.ifaceMethod(), injector.method());
			} else if (injector.ifaceMethod().returns(BOOLEAN)) {
				this.instrumentInjectorAsCancellation(injector.ifaceMethod(), injector.method());
			} else {
				throw CrashReport.create("Unknown how to implement injector, tried as listener, callback and cancellation but failed", IMPLEMENTATION_ERROR).addDetail("Injector", injector.ifaceMethod().getMethodSignature())
					.addDetail("Method", injector.method().getMethodSignature()).addDetail("Line", injector.line()).exception();
			}
		}
		
		private void instrumentInjectorAsListener(@NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			this.instrumentMethodCall(ifaceMethod, method);
			this.markModified.run();
		}
		
		private void instrumentInjectorAsCallback(@NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodCall(ifaceMethod, method);
			int local = Instrumentations.newLocal(this.mv, method.getReturnType());
			this.mv.visitLabel(start);
			this.mv.visitVarInsn(Opcodes.ASTORE, local);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNULL, end);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			if (method.returnsAny(PRIMITIVES)) {
				Type primitive = method.getReturnType();
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ifaceMethod.getReturnType().getInternalName(), primitive.getClassName() + "Value", "()" + primitive.getDescriptor(), false);
			}
			this.mv.visitInsn(method.getReturnType().getOpcode(Opcodes.IRETURN));
			this.mv.visitJumpInsn(Opcodes.GOTO, end);
			this.mv.visitLabel(end);
			this.mv.visitLocalVariable("generated$InjectorTransformer$Temp" + local, method.getReturnType().getDescriptor(), null, start, end, local);
			this.markModified.run();
		}
		
		private void instrumentInjectorAsCancellation(@NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodCall(ifaceMethod, method);
			int local = Instrumentations.newLocal(this.mv, BOOLEAN);
			this.mv.visitLabel(start);
			this.mv.visitVarInsn(Opcodes.ISTORE, local);
			this.mv.visitVarInsn(Opcodes.ILOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNE, end);
			this.mv.visitInsn(Opcodes.RETURN);
			this.mv.visitJumpInsn(Opcodes.GOTO, start);
			this.mv.visitLabel(end);
			this.mv.visitLocalVariable("generated$InjectorTransformer$Temp" + local, "Z", null, start, end, local);
			this.markModified.run();
		}
		
		private void instrumentMethodCall(@NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			boolean isInstance = !ifaceMethod.is(TypeModifier.STATIC);
			if (isInstance) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			for (ParameterData parameter : ifaceMethod.parameters()) {
				this.mv.visitVarInsn(parameter.type().getOpcode(Opcodes.ILOAD), this.getLoadIndex(parameter, ifaceMethod, method));
			}
			this.mv.visitMethodInsn(isInstance ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKESTATIC, ifaceMethod.owner().getInternalName(), ifaceMethod.name(), ifaceMethod.type().getDescriptor(), true);
		}
		//endregion
		
		//region Helper methods
		private int getLoadIndex(@NotNull ParameterData parameter, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			if (parameter.isAnnotatedWith(THIS)) {
				this.checkStatic(parameter, ifaceMethod, method);
				return 0;
			}
			AnnotationData annotation = parameter.getAnnotation(LOCAL);
			String value = annotation.getOrDefault("value");
			if (value.isEmpty()) {
				if (!parameter.isNamed()) {
					throw CrashReport.create("Unable to map injector parameter to target by name, because the parameter name was not included into the class file during compilation", MISSING_INFORMATION)
						.addDetail("Injector", ifaceMethod.getMethodSignature()).addDetail("Method", method.getMethodSignature()).addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
				}
				String name = parameter.name();
				if ("_this".equals(name)) {
					this.checkStatic(parameter, ifaceMethod, method);
					System.out.println("Found parameter which specifies 'this' as target using the @Local annotation, use the @This annotation instead");
					return 0;
				}
				int index = this.getLoadIndex(name, parameter, ifaceMethod, method);
				if (index != -1) {
					return index;
				}
			} else if (value.chars().allMatch(Character::isDigit)) {
				int index = Integer.parseInt(value);
				int max = method.getParameterCount() + method.getLocalVariableCount();
				if (!method.is(TypeModifier.STATIC)) {
					max++;
				}
				if (index >= max) {
					throw CrashReport.create("Unable to map injector parameter to target by index, because the index is out of bounds", MISSING_INFORMATION).addDetail("Injector", ifaceMethod.getMethodSignature())
						.addDetail("Method", method.getMethodSignature()).addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).addDetail("Index", index).addDetail("Max", max).exception();
				}
				return index;
			} else if ("this".equals(value)) {
				this.checkStatic(parameter, ifaceMethod, method);
				System.out.println("Found parameter which specifies 'this' as target using the @Local annotation, use the @This annotation instead");
				return 0;
			} else {
				int index = this.getLoadIndex(value, parameter, ifaceMethod, method);
				if (index != -1) {
					return index;
				}
			}
			throw CrashReport.create("Unable to find target for injector parameter", IMPLEMENTATION_ERROR).addDetail("Injector", ifaceMethod.getMethodSignature()).addDetail("Method", method.getMethodSignature())
				.addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).addDetail("Local Annotation Value", value).exception();
		}
		
		private void checkStatic(@NotNull ParameterData parameter, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			if (method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Unable to map injector parameter to 'this', because the method is static", MISSING_INFORMATION).addDetail("Injector", ifaceMethod.getMethodSignature())
					.addDetail("Method", method.getMethodSignature()).addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
			}
		}
		
		private int getLoadIndex(@NotNull String value, @NotNull ParameterData parameter, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			for (ParameterData param : method.parameters()) {
				if (!param.isNamed()) {
					throw CrashReport.create("Unable to find target by name for injector parameter, because the name was not included into the class file during compilation", MISSING_INFORMATION)
						.addDetail("Injector", ifaceMethod.getMethodSignature()).addDetail("Method", method.getMethodSignature()).addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
				}
				if (param.name().equals(value)) {
					return method.is(TypeModifier.STATIC) ? param.index() : param.index() + 1;
				}
			}
			if (method.localVariables().isEmpty()) {
				throw CrashReport.create("Unable to find target by name for injector parameter, because the local variables were not included into the class file during compilation", MISSING_INFORMATION)
					.addDetail("Injector", ifaceMethod.getMethodSignature()).addDetail("Method", method.getMethodSignature()).addDetail("Parameter Index", parameter.index()).addDetail("Parameter Type", parameter.type()).exception();
			}
			for (LocalVariableData local : method.getLocalVariables()) {
				if (local.name().equals(value)) {
					return local.index();
				}
			}
			return -1;
		}
		//endregion
		
		static {
			try {
				LABEL_LINE = Label.class.getDeclaredField("lineNumber");
				LABEL_LINE.setAccessible(true);
				LABEL_FLAGS = Label.class.getDeclaredField("flags");
				LABEL_FLAGS.setAccessible(true);
			} catch (NoSuchFieldException e) {
				throw new ExceptionInInitializerError(e);
			}
		}
	}
	
	private record InjectorData(int line, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {}
}
