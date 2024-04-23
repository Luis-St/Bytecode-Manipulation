package net.luis.preload.data;

import net.luis.preload.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Luis-St
 *
 */

public record ClassInfo(@NotNull String name, @NotNull Type type, String signature, @NotNull TypeAccess access, @NotNull ClassType classType, @NotNull List<TypeModifier> modifiers,
						@Nullable Type superType, @NotNull List<Type> interfaces, @NotNull Map<Type, AnnotationData> annotations) implements ASMData {}
