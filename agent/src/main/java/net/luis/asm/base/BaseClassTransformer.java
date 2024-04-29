package net.luis.asm.base;

import net.luis.asm.report.CrashReport;
import net.luis.asm.report.ReportedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public abstract class BaseClassTransformer implements ClassFileTransformer {
	
	@Override
	public final byte @Nullable [] transform(@NotNull ClassLoader loader, @NotNull String className, @Nullable Class<?> clazz, @NotNull ProtectionDomain domain, byte @NotNull [] buffer) {
		ClassReader reader = new ClassReader(buffer);
		ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
		ClassVisitor visitor = this.visit(className, clazz, reader, writer);
		try {
			reader.accept(visitor, ClassReader.EXPAND_FRAMES);
			return writer.toByteArray();
		} catch (Throwable throwable) {
			CrashReport report;
			if (throwable instanceof ReportedException ex) {
				report = ex.getReport();
			} else {
				report = CrashReport.create("Error occurred while transforming class '" + Type.getObjectType(className) + "'", throwable);
			}
			report.addDetailFirst("Transformed Class", Type.getObjectType(className));
			report.addDetailFirst("Class Transformer", this.getClass().getSimpleName());
			report.addDetailFirst("Class Loader", loader.getName());
			report.print();
			if (!report.canContinue()) {
				System.exit(report.getExitCode());
			}
		}
		return buffer;
	}
	
	protected abstract @NotNull ClassVisitor visit(@NotNull String className, @Nullable Class<?> clazz, @NotNull ClassReader reader, @NotNull ClassWriter writer);
}
