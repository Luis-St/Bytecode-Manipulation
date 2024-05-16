package net.luis.agent.asm.base;

import net.luis.agent.asm.ASMUtils;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.report.ReportedException;
import net.luis.agent.preload.PreloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseClassTransformer implements ClassFileTransformer {
	
	private static final List<String> IGNORED_CLASSES = List.of(
		"java/", "javax/", "sun/", "com/sun/", "jdk/", // Java
		"org/jetbrains/annotations/", "org/intellij/lang/annotations/", // JetBrains
		"org/objectweb/asm/", // ASM
		"net/luis/agent/" // Agent
	);
	
	protected final PreloadContext context;
	private final boolean computeFrames;
	protected boolean modified;
	
	protected BaseClassTransformer(@NotNull PreloadContext context) {
		this(context, false);
	}
	
	protected BaseClassTransformer(@NotNull PreloadContext context, boolean computeFrames) {
		this.context = context;
		this.computeFrames = computeFrames;
	}
	
	protected boolean shouldTransform(@NotNull Type type) {
		for (String ignored : IGNORED_CLASSES) {
			if (type.getInternalName().startsWith(ignored)) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public final byte @Nullable [] transform(@NotNull ClassLoader loader, @NotNull String className, @Nullable Class<?> clazz, @NotNull ProtectionDomain domain, byte @NotNull [] buffer) {
		Type type = Type.getObjectType(className);
		if (!this.shouldTransform(type)) {
			return null;
		}
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, this.computeFrames ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = this.visit(type, clazz, reader, writer);
		try {
			reader.accept(visitor, ClassReader.EXPAND_FRAMES);
			byte[] bytes = writer.toByteArray();
			if (this.modified) {
				System.out.println("Transformed Class: " + type);
				ASMUtils.saveClass(new File("transformed/" + className + ".class"), bytes);
				this.modified = false;
			}
			return bytes;
		} catch (Throwable throwable) {
			CrashReport report;
			if (throwable instanceof ReportedException ex) {
				report = ex.getReport();
			} else {
				report = CrashReport.create("Error occurred while transforming class '" + type + "'", throwable);
			}
			report.addDetailFirst("Transformed Class", type);
			report.addDetailFirst("Class Transformer", this.getClass().getSimpleName());
			report.addDetailFirst("Class Loader", loader.getName());
			report.print();
			if (!report.canContinue()) {
				System.exit(report.getExitCode());
			}
		}
		return null;
	}
	
	protected abstract @NotNull ClassVisitor visit(@NotNull Type type, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer);
}
