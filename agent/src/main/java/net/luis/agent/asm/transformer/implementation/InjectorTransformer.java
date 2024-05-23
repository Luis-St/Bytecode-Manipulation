package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.scanner.ClassFileScanner;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.InjectMode;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

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
	
	@Override
	protected boolean shouldTransform(@NotNull Type type) {
		return super.shouldTransform(type) || this.lookup.containsKey(type.getInternalName());
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassWriter writer) {
		return new InjectorClassVisitor(writer, this.context, type, () -> this.modified = true, this.lookup);
	}
	
	private static class InjectorClassVisitor extends BaseClassVisitor {
		
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
		
		// ToDo: ifaceMethod must return none primitive type -> must be nullable -> support for primitive return types
		// ToDo: add support for inject into static methods
		
		private void validateMethod(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type target, @NotNull ClassContent targetContent) {
			String signature = ifaceMethod.getMethodSignature();
			//region Base validation
			if (ifaceMethod.access() != TypeAccess.PUBLIC) {
				throw CrashReport.create("Method annotated with @Injector must be public", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).exception();
			}
			if (ifaceMethod.is(TypeModifier.STATIC)) {
				throw CrashReport.create("Method annotated with @Injector must not be static", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).exception();
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
			if (method.returns(VOID)) {
				if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(BOOLEAN)) {
					throw CrashReport.create("Method annotated with @Injector specified a method which returns void, but injector method does not return void or boolean (cancellation)", REPORT_CATEGORY).addDetail("Interface", iface)
						.addDetail("Injector", signature).addDetail("Injector Return Type", method.getReturnType()).addDetail("Method", method.getMethodSignature()).exception();
				}
			} else if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(method.getReturnType())) {
				throw CrashReport.create("Method annotated with @Injector must either return void or the same type as the specified method", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Injector Return Type", ifaceMethod.getReturnType()).addDetail("Method", method.getMethodSignature()).addDetail("Method Return Type", method.getReturnType()).exception();
			}
			String injectorTarget = Objects.requireNonNull(ifaceMethod.getAnnotation(INJECTOR).get("target"));
			int ordinal = ifaceMethod.getAnnotation(INJECTOR).getOrDefault(this.context, "ordinal");
			InjectMode mode = InjectMode.valueOf(ifaceMethod.getAnnotation(INJECTOR).getOrDefault(this.context, "mode"));
			InjectorScanClassVisitor scanner = new InjectorScanClassVisitor(method, injectorTarget, ordinal);
			ClassFileScanner.scanClassCustom(this.type, scanner);
			
			if (!scanner.visitedTarget()) {
				throw CrashReport.create("Could not find method specified in injector during scan of its own class", REPORT_CATEGORY).addDetail("Scanner", scanner).addDetail("Interface", iface)
					.addDetail("Injector", signature).addDetail("Scanned Class", target).addDetail("Method", method.getMethodSignature()).exception();
			}
			int line = scanner.getLine();
			if (line == -1) {
				throw CrashReport.create("Could not find target method in method body of method specified in injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Method", method.getMethodSignature()).addDetail("Target", injectorTarget).addDetail("Ordinal", ordinal).exception();
			}
			this.injectors.computeIfAbsent(method.getMethodSignature(), m -> new ArrayList<>()).add(new InjectorData(line, mode, iface, ifaceMethod, target, method));
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
		
		private final Map</*Line Number*/Integer, List<InjectorData>> injectors;
		
		private InjectorMethodVisitor(@NotNull LocalVariablesSorter visitor, @NotNull List<InjectorData> injectors, @NotNull Runnable markModified) {
			super(visitor, markModified);
			this.injectors = new HashMap<>();
			for (InjectorData data : injectors) {
				this.injectors.computeIfAbsent(data.line(), l -> new ArrayList<>()).add(data);
			}
		}
		
		@Override
		public void visitLineNumber(int line, @NotNull Label start) {
			this.injectors.getOrDefault(line, new ArrayList<>()).stream().filter(data -> data.mode() == InjectMode.BEFORE).forEach(this::instrumentInjector);
			super.visitLineNumber(line, start);
			this.injectors.getOrDefault(line, new ArrayList<>()).stream().filter(data -> data.mode() == InjectMode.AFTER).forEach(this::instrumentInjector);
		}
		
		private void instrumentInjector(@NotNull InjectorData injector) {
			if (injector.ifaceMethod().returns(VOID)) {
				this.instrumentInjectorAsListener(injector.iface(), injector.ifaceMethod());
			} else if (injector.ifaceMethod().returns(injector.method().getReturnType())) {
				this.instrumentInjectorAsCallback(injector.iface(), injector.ifaceMethod(), injector.method());
			} else if (injector.ifaceMethod().returns(BOOLEAN)) {
				this.instrumentInjectorAsCancellation(injector.iface(), injector.ifaceMethod());
			} else {
				throw CrashReport.create("Unknown how to implement injector, tried as listener, callback and cancellation but failed", REPORT_CATEGORY).addDetail("Injector", injector.ifaceMethod().getMethodSignature())
					.addDetail("Method", injector.method().getMethodSignature()).addDetail("Mode", Utils.capitalize(injector.mode().name())).addDetail("Line", injector.line()).exception();
			}
		}
		
		private void instrumentInjectorAsListener(@NotNull Type iface, @NotNull MethodData ifaceMethod) {
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, iface.getInternalName(), ifaceMethod.name(), ifaceMethod.type().getDescriptor(), true);
			this.markModified();
		}
		
		private void instrumentInjectorAsCallback(@NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull MethodData method) {
			Label label = new Label();
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, iface.getInternalName(), ifaceMethod.name(), ifaceMethod.type().getDescriptor(), true);
			int local = this.newLocal(method.getReturnType());
			this.mv.visitVarInsn(Opcodes.ASTORE, local);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNULL, label);
			this.mv.visitVarInsn(Opcodes.ALOAD, local);
			this.mv.visitInsn(Opcodes.ARETURN);
			this.mv.visitJumpInsn(Opcodes.GOTO, label);
			this.mv.visitLabel(label);
			this.markModified();
		}
		
		private void instrumentInjectorAsCancellation(@NotNull Type iface, @NotNull MethodData ifaceMethod) {
			Label label = new Label();
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, iface.getInternalName(), ifaceMethod.name(), ifaceMethod.type().getDescriptor(), true);
			int local = this.newLocal(BOOLEAN);
			this.mv.visitVarInsn(Opcodes.ISTORE, local);
			this.mv.visitVarInsn(Opcodes.ILOAD, local);
			this.mv.visitJumpInsn(Opcodes.IFNE, label);
			this.mv.visitInsn(Opcodes.RETURN);
			this.mv.visitLabel(label);
			this.markModified();
		}
	}
	
	private record InjectorData(int line, @NotNull InjectMode mode, @NotNull Type iface, @NotNull MethodData ifaceMethod, @NotNull Type type, @NotNull MethodData method) {}
	
	//region Injector scan
	private static class InjectorScanClassVisitor extends ClassVisitor {
		
		private final MethodData injectorMethod;
		private final String target;
		private final int ordinal;
		private InjectorScanMethodVisitor visitor;
		
		private InjectorScanClassVisitor(@NotNull MethodData injectorMethod, @NotNull String target, int ordinal) {
			super(Opcodes.ASM9);
			this.injectorMethod = injectorMethod;
			this.target = target;
			this.ordinal = ordinal;
		}
		
		@Override
		public @NotNull MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String descriptor, @Nullable String signature, String @Nullable [] exceptions) {
			if (this.injectorMethod.getMethodSignature().equals(name + descriptor)) {
				this.visitor = new InjectorScanMethodVisitor(this.target, this.ordinal);
				return this.visitor;
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
		
		public boolean visitedTarget() {
			return this.visitor != null;
		}
		
		public int getLine() {
			return this.visitor == null ? -1 : this.visitor.getLine();
		}
	}
	
	private static class InjectorScanMethodVisitor extends BaseMethodVisitor {
		
		private final String target;
		private final int ordinal;
		private int targetLine = -1;
		private int currentLine;
		private int visited;
		
		private InjectorScanMethodVisitor(@NotNull String target, int ordinal) {
			super(() -> {});
			this.target = target;
			this.ordinal = ordinal;
		}
		
		@Override
		public void visitLineNumber(int line, @NotNull Label start) {
			super.visitLineNumber(line, start);
			this.currentLine = line;
		}
		
		@Override
		public void visitMethodInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String descriptor, boolean isInterface) {
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			if (ASMUtils.matchesTarget(this.target, Type.getObjectType(owner), name, Type.getType(descriptor))) {
				if (this.visited == this.ordinal && this.targetLine == -1) {
					this.targetLine = this.currentLine;
				} else {
					this.visited++;
				}
			}
		}
		
		public int getLine() {
			return this.targetLine;
		}
	}
	//endregion
}
