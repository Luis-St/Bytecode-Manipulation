package net.luis.agent.asm.transformer;

import net.luis.agent.Agent;
import net.luis.agent.asm.base.BaseClassTransformer;
import net.luis.agent.asm.data.Class;
import net.luis.agent.asm.data.*;
import net.luis.agent.asm.report.CrashReport;
import net.luis.agent.asm.type.SignatureType;
import net.luis.agent.asm.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.*;

import static net.luis.agent.asm.Types.*;

/**
 * @author Luis-St
 */

public class ValidationTransformer extends BaseClassTransformer {
	
	private static final String REPORT_CATEGORY = "Validation Error";
	
	@Override
	protected @NotNull ClassVisitor visit(@NotNull Type type, @NotNull ClassWriter writer) {
		Class clazz = Agent.getClass(type);
		Class immutableClass = this.findImmutableClass(clazz);
		if (immutableClass != null) {
			this.checkClassImmutability(immutableClass, clazz);
		}
		return new ClassVisitor(Opcodes.ASM9, writer) {};
	}
	
	//region Helper methods
	private @Nullable Class findImmutableClass(@NotNull Class clazz) {
		if (clazz.isAnnotatedWith(IMMUTABLE)) {
			return clazz;
		}
		while (clazz.getSuperType() != null) {
			clazz = Agent.getClass(clazz.getSuperType());
			if (clazz.isAnnotatedWith(IMMUTABLE)) {
				Annotation annotation = clazz.getAnnotation(IMMUTABLE);
				if ((boolean) annotation.getOrDefault("inherit")) {
					return clazz;
				}
			}
		}
		return null;
	}
	
	private void checkClassImmutability(@NotNull Class immutableClass, @NotNull Class clazz) {
		for (Field field : clazz.getFields().values()) {
			if (field.is(TypeModifier.FINAL)) {
				continue;
			}
			
			CrashReport report = CrashReport.create(REPORT_CATEGORY).addDetail("Immutable Class", immutableClass.getType()).addDetail("Field", field.getSignature(SignatureType.DEBUG));
			if (immutableClass.equals(clazz)) {
				throw report.exception("Class annotated with @Immutable must only contain final fields");
			} else {
				throw report.exception("A class which inherits a class annotated with @Immutable must only contain final fields");
			}
		}
	}
	//endregion
}
