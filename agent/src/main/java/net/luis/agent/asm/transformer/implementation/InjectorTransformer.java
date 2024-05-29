package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseMethodVisitor;
import net.luis.agent.asm.base.visitor.ContextBasedClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
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
	
	private static final String REPORT_CATEGORY = "Inject Implementation Error";
	
	private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup = ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE);
	
	public InjectorTransformer(@NotNull PreloadContext context) {
		super(context, true);
	}
	
	//region Type filtering
	@Override
	protected boolean shouldIgnoreClass(@NotNull Type type) {
		return !this.lookup.containsKey(type.getInternalName());
	}
	//endregion
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new InjectorClassVisitor(writer, this.context, type, () -> this.modified = true, this.lookup);
	}
	
	private static class InjectorClassVisitor extends ContextBasedClassVisitor {
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		private final Map</*Method Signature*/String, List<InjectorData>> injectors = new HashMap<>();
		
		private InjectorClassVisitor(@NotNull ClassWriter writer, @NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(writer, context, type, markModified);
			this.lookup = lookup;
		}
		
		private static @NotNull CrashReport createReport(@NotNull String message, @NotNull Type iface, @NotNull String methodSignature) {
			return CrashReport.create(message, REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Interface Method", methodSignature);
		}
		
		@Override
		@SuppressWarnings("DuplicatedCode")
		public void visit(int version, int access, @NotNull String name, @Nullable String signature, @Nullable String superClass, String @Nullable [] interfaces) {
			super.visit(version, access, name, signature, superClass, interfaces);
			if (this.lookup.containsKey(name)) {
				Type target = Type.getObjectType(name);
				ClassContent targetContent = this.context.getClassContent(target);
				for (Type iface : this.lookup.get(name).stream().map(Type::getObjectType).toList()) {
					ClassContent ifaceContent = this.context.getClassContent(iface);
					for (MethodData method : ifaceContent.methods()) {
						if (method.isAnnotatedWith(INJECTOR)) {
							this.validateMethod(iface, method, target, targetContent);
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
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("Method annotated with @Injector must be public", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.ABSTRACT)) {
				throw CrashReport.create("Method annotated with @Injector must be default implemented", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).exception();
			}
			//endregion
			if (ifaceMethod.getExceptionCount() > 0) {
				throw CrashReport.create("Method annotated with @Injector must not throw exceptions", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Exceptions", ifaceMethod.exceptions()).exception();
			}
			MethodData existingMethod = targetContent.getMethod(ifaceMethod.name(), ifaceMethod.type());
			if (existingMethod != null) {
				throw CrashReport.create("Target class of injector already has method with same signature", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Existing Method", existingMethod.getMethodSignature()).exception();
			}
			String injectorName = this.getInjectorName(ifaceMethod);
			List<MethodData> possibleMethod = ASMUtils.getBySignature(injectorName, targetContent);
			if (possibleMethod.isEmpty()) {
				throw CrashReport.create("Could not find method specified in injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).addDetail("Method", injectorName)
					.addDetail("Possible Methods", targetContent.getMethods(this.getRawInjectorName(injectorName)).stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).addDetail("Method", injectorName)
					.addDetail("Possible Methods", possibleMethod.stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			MethodData method = possibleMethod.getFirst();
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				if (!method.is(TypeModifier.STATIC)) {
					throw CrashReport.create("Method annotated with @Injector is declared static, but specified a none-static method", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
						.addDetail("Method", method.getMethodSignature()).exception();
				}
			} else if (method.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Injector is declared none-static, but specified a static method", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Method", method.getMethodSignature()).exception();
			}
			if (method.returns(VOID)) {
				if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(BOOLEAN)) {
					throw CrashReport.create("Method annotated with @Injector specified a method which returns void, but injector method does not return void or boolean (cancellation)", REPORT_CATEGORY).addDetail("Interface", iface)
						.addDetail("Injector", signature).addDetail("Injector Return Type", method.getReturnType()).addDetail("Method", method.getMethodSignature()).exception();
				}
			} else if (method.returnsAny(PRIMITIVES)) {
				Type methodWrapper = convertToWrapper(method.getReturnType());
				if (!ifaceMethod.returns(methodWrapper)) {
					throw CrashReport.create("Method annotated with @Injector specified a method which returns a primitive type, but injector method does not return the corresponding wrapper type", REPORT_CATEGORY)
						.addDetail("Interface", iface).addDetail("Injector", signature).addDetail("Injector Return Type", methodWrapper).addDetail("Method", method.getMethodSignature())
						.addDetail("Method Return Type", method.getReturnType()).exception();
				}
			} else if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(method.getReturnType())) {
				throw CrashReport.create("Method annotated with @Injector must either return void or the same type as the specified method", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Injector Return Type", ifaceMethod.getReturnType()).addDetail("Method", method.getMethodSignature()).addDetail("Method Return Type", method.getReturnType()).exception();
			}
			
			AnnotationData annotation = Objects.requireNonNull(ifaceMethod.getAnnotation(INJECTOR).get("target"));
			TargetClassScanner scanner = new TargetClassScanner(this.context, method, annotation);
			ClassFileScanner.scanClassCustom(this.type, scanner);
			if (!scanner.visitedTarget()) {
				throw CrashReport.create("Could not find method specified in injector during scan of its own class", REPORT_CATEGORY).addDetail("Scanner", scanner).addDetail("Interface", iface)
					.addDetail("Injector", signature).addDetail("Scanned Class", target).addDetail("Method", method.getMethodSignature()).exception();
			}
			int line = scanner.getLine();
			if (line == -1) {
				throw CrashReport.create("Could not find target in method body of method specified in injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Method", method.getMethodSignature()).addDetail("Target", annotation.get("value")).addDetail("Target Type", annotation.get("type")).addDetail("Target Mode", annotation.getOrDefault(this.context, "mode"))
					.addDetail("Target Ordinal", annotation.getOrDefault(this.context, "ordinal")).addDetail("Target Offset", annotation.getOrDefault(this.context, "offset")).exception();
			}
			
			this.injectors.computeIfAbsent(method.getMethodSignature(), m -> new ArrayList<>()).add(new InjectorData(line, iface, ifaceMethod, method));
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
	
	private static class InjectorMethodVisitor extends BaseMethodVisitor {
		
		private static final int FLAG_LINE_NUMBER = 128;
		private static final Field LABEL_LINE;
		private static final Field LABEL_FLAGS;
		
		private final Map</*Line Number*/Integer, List<InjectorData>> injectors;
		private int lastLine = -1;
		
		private InjectorMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull List<InjectorData> injectors, @NotNull Runnable markModified) {
			super(visitor, markModified);
			this.injectors = new HashMap<>();
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
						List<InjectorData> injectors = this.injectors.get(i);
						if (injectors.size() == 1) {
							InjectorData injector = injectors.getFirst();
							throw CrashReport.create("Injector was not instrumented correctly, because no label was found", REPORT_CATEGORY).addDetail("Line", i).addDetail("Interface", injector.iface().getInternalName())
								.addDetail("Injector", injector.ifaceMethod().getMethodSignature()).addDetail("Method", injector.method().getMethodSignature()).exception();
						} else {
							CrashReport report = CrashReport.create("Injectors were not instrumented correctly, because no label was found", REPORT_CATEGORY);
							
							List<Map<String, Object>> details = new ArrayList<>();
							for (InjectorData injector : injectors) {
								Map<String, Object> detail = new HashMap<>();
								detail.put("Interface", injector.iface().getInternalName());
								detail.put("Injector", injector.ifaceMethod().getMethodSignature());
								detail.put("Method", injector.method().getMethodSignature());
								details.add(detail);
							}
							throw report.addDetail("Line", i).addDetail("Injectors", details).exception();
						}
					}
				}
			}
			this.instrumentInLine(line);
			this.lastLine = line;
		}
		
		private void instrumentInjector(@NotNull InjectorData injector) {
			if (injector.ifaceMethod().returns(VOID)) {
				this.instrumentInjectorAsListener(injector.iface(), injector.ifaceMethod());
			} else if (injector.method().returns(convertToPrimitive(injector.ifaceMethod().getReturnType()))) {
				this.instrumentInjectorAsCallback(injector.iface(), injector.ifaceMethod(), injector.method());
			} else if (injector.ifaceMethod().returns(BOOLEAN)) {
				this.instrumentInjectorAsCancellation(injector.iface(), injector.ifaceMethod());
			} else {
				throw CrashReport.create("Unknown how to implement injector, tried as listener, callback and cancellation but failed", REPORT_CATEGORY).addDetail("Injector", injector.ifaceMethod().getMethodSignature())
					.addDetail("Method", injector.method().getMethodSignature()).addDetail("Line", injector.line()).exception();
			}
		}
		
		private void instrumentInjectorAsListener(@NotNull Type iface, @NotNull MethodData ifaceMethod) {
			this.instrumentMethodCall(this.mv, iface, ifaceMethod, true);
			this.markModified();
		}
		
		private void instrumentInjectorAsCallback(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodCall(this.mv, iface, ifaceMethod, true);
			int local = this.newLocal(method.getReturnType());
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
			this.markModified();
		}
		
		private void instrumentInjectorAsCancellation(@NotNull Type iface, @NotNull MethodData ifaceMethod) {
			Label start = new Label();
			Label end = new Label();
			this.instrumentMethodCall(this.mv, iface, ifaceMethod, true);
			int local = this.newLocal(BOOLEAN);
			this.mv.visitLabel(start);
			this.mv.visitVarInsn(Opcodes.ISTORE, local);
			this.mv.visitVarInsn(Opcodes.ILOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNE, end);
			this.mv.visitInsn(Opcodes.RETURN);
			this.mv.visitJumpInsn(Opcodes.GOTO, start);
			this.mv.visitLabel(end);
			this.mv.visitLocalVariable("generated$InjectorTransformer$Temp" + local, "Z", null, start, end, local);
			this.markModified();
		}
		
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
	
	private record InjectorData(int line, @NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {}
}
