package net.luis.preload.data;

import net.luis.preload.type.TypeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;

/**
 *
 * @author Luis-St
 *
 */

public interface ASMData {
	
	@NotNull String name();
	
	@NotNull Type type();
	
	@Nullable String signature();
	
	@NotNull List<TypeModifier> modifiers();
	
	@NotNull List<AnnotationData> annotations();
}
