package net.luis.agent.asm.transformer.implementation;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.base.visitor.BaseClassVisitor;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.preload.PreloadContext;
import net.luis.agent.preload.data.*;
import net.luis.agent.preload.scanner.ClassFileScanner;
import net.luis.agent.preload.type.TypeAccess;
import net.luis.agent.preload.type.TypeModifier;
import net.luis.agent.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.util.*;

import static net.luis.agent.asm.Types.*;

/**
 *
 * @author Luis-St
 *
 */

public class InjectorTransformer extends BaseClassTransformer {
	
	public InjectorTransformer(@NotNull PreloadContext context) {
		super(context);
	}
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer) {
		return new InjectorClassVisitor(this.context, type, () -> this.modified = true, ASMUtils.createTargetsLookup(this.context, INJECT_INTERFACE));
	}
	
	private static class InjectorClassVisitor extends BaseClassVisitor {
		
		private static final String REPORT_CATEGORY = "Inject Implementation Error";
		
		private final Map</*Target Class*/String, /*Interfaces*/List<String>> lookup;
		
		private InjectorClassVisitor(@NotNull PreloadContext context, @NotNull Type type, @NotNull Runnable markModified, @NotNull Map</*Target Class*/String, /*Interfaces*/List<String>> lookup) {
			super(context, type, markModified);
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
				throw CrashReport.create("Could not find method specified in injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).addDetail("Injector Name", injectorName)
					.addDetail("Possible Methods", targetContent.getMethods(this.getRawInjectorName(injectorName)).stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			if (possibleMethod.size() > 1) {
				throw CrashReport.create("Found multiple possible methods for injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Invoker", signature).addDetail("Injector Name", injectorName)
					.addDetail("Possible Methods", possibleMethod.stream().map(MethodData::getMethodSignature).toList()).exception();
			}
			MethodData injectorMethod = possibleMethod.getFirst();
			if (injectorMethod.returns(VOID)) {
				if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(BOOLEAN)) {
					throw CrashReport.create("Method annotated with @Injector specified method which returns void, but interface method does not return void or boolean", REPORT_CATEGORY).addDetail("Interface", iface)
						.addDetail("Injector", signature).addDetail("Injector Return Type", injectorMethod.getReturnType()).exception();
				}
			} else if (!ifaceMethod.returns(VOID) && !ifaceMethod.returns(injectorMethod.getReturnType())) {
				throw CrashReport.create("Method annotated with @Injector must either return void or the same type as the target method", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature)
					.addDetail("Injector Return Type", ifaceMethod.getReturnType()).addDetail("Target Return Type", injectorMethod.getReturnType()).exception();
			}
			String injectorTarget = Objects.requireNonNull(ifaceMethod.getAnnotation(INJECTOR).get("target"));
			int ordinal = ifaceMethod.getAnnotation(INJECTOR).getOrDefault(this.context, "ordinal");
			int line = ClassFileScanner.scanClass(this.type, new InjectorScanClassVisitor(injectorMethod, injectorTarget, ordinal), InjectorScanClassVisitor::getLine);
			if (line == -1) {
				throw CrashReport.create("Could not find target method in method body of injector", REPORT_CATEGORY).addDetail("Interface", iface).addDetail("Injector", signature).addDetail("Target", injectorTarget)
					.addDetail("Ordinal", ordinal).exception();
			}
		}
	}
	
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
			if (this.injectorMethod.getMethodSignature().equalsIgnoreCase(name + descriptor)) {
				this.visitor = new InjectorScanMethodVisitor(this.target, this.ordinal);
				return this.visitor;
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}
		
		public int getLine() {
			return this.visitor == null ? -1 : this.visitor.getLine();
		}
		
		public boolean visitedTarget() {
			return this.visitor != null;
		}
	}
	
	private static class InjectorScanMethodVisitor extends MethodVisitor {
		
		private final String target;
		private final int ordinal;
		private int targetLine = -1;
		private int currentLine;
		private int visited;
		
		private InjectorScanMethodVisitor(@NotNull String target, int ordinal) {
			super(Opcodes.ASM9);
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
				if (this.visited == this.ordinal) {
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
