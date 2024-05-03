package net.luis.agent.preload.data;

import net.luis.agent.preload.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.*;

/**
 *
 * @author Luis-St
 *
 */

public record ClassInfo(@NotNull String name, @NotNull Type type, String signature, @NotNull TypeAccess access, @NotNull ClassType classType, @NotNull Set<TypeModifier> modifiers,
						@Nullable Type superType, @NotNull List<Type> interfaces, @NotNull Map<Type, AnnotationData> annotations) implements ASMData {}
